package app.familygem;

import static app.familygem.Alberi.apriGedcom;
import static app.familygem.Global.context;
import static app.familygem.Global.gc;
import static app.familygem.Global.settings;
import static graph.gedcom.Util.HEARTH_DIAMETER;
import static graph.gedcom.Util.MARRIAGE_HEIGHT;
import static graph.gedcom.Util.MINI_HEARTH_DIAMETER;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.familygem.action.CheckAsCollaboratorTask;
import com.familygem.action.CreateRepoTask;
import com.familygem.action.ForkRepoTask;
import com.familygem.action.GetMyReposTask;
import com.familygem.action.RedownloadRepoTask;
import com.familygem.action.SaveInfoFileTask;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.familygem.utility.PrivatePerson;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.familygem.constants.Gender;
import app.familygem.constants.Relation;
import app.familygem.dettaglio.Famiglia;
import graph.gedcom.Bond;
import graph.gedcom.CurveLine;
import graph.gedcom.FamilyNode;
import graph.gedcom.Graph;
import graph.gedcom.Line;
import graph.gedcom.Metric;
import graph.gedcom.PersonNode;
import app.familygem.R;
public class Diagram extends Fragment {

	private Graph graph;
	private MoveLayout moveLayout;
	private RelativeLayout box;
	private GraphicPerson fulcrumView;
	private Person fulcrum;
	private FulcrumGlow glow;
	private Lines lines;
	private Lines backLines;
	private float density;
	private int STROKE;
	private final int GLOW_SPACE = 35; // Space to display glow, in dp
	private View popup; // Suggestion balloon
	boolean forceDraw;
	private Timer timer;
	private boolean play;
	private AnimatorSet animator;
	private boolean printPDF; // We are exporting a PDF
	private final boolean leftToRight = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_LTR;

	private static boolean redirectEdit = true;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		density = getResources().getDisplayMetrics().density;
		STROKE = toPx(2);

		getActivity().findViewById(R.id.toolbar).setVisibility(View.GONE); // Necessario in caso di backPressed dopo onActivityresult
		final View view = inflater.inflate(R.layout.diagram, container, false);
		view.findViewById(R.id.diagram_hamburger).setOnClickListener(v -> {
			DrawerLayout scatolissima = getActivity().findViewById(R.id.scatolissima);
			scatolissima.openDrawer(GravityCompat.START);
		});
		view.findViewById(R.id.diagram_options).setOnClickListener(vista -> {
			PopupMenu opzioni = new PopupMenu(getContext(), vista);
			Menu menu = opzioni.getMenu();
			menu.add(0, 0, 0, R.string.diagram_settings);
			if( gc.getPeople().size() > 0 ) {
				menu.add(0, 1, 0, R.string.export_pdf);
				menu.add(0, 2, 0, R.string.find_person);
			}
			opzioni.show();
			opzioni.setOnMenuItemClickListener(item -> {
				switch( item.getItemId() ) {
					case 0: // Diagram settings
						startActivity(new Intent(getContext(), DiagramSettings.class));
						break;
					case 1: // Export PDF
						F.salvaDocumento(null, this, Global.settings.openTree, "application/pdf", "pdf", 903);
						break;
					case 2: // Find person
						Intent searchIntent = new Intent(getContext(), SearchPersonActivity.class);
						startActivityForResult(searchIntent, 904);
						break;
					default:
						return false;
				}
				return true;
			});
		});

		moveLayout = view.findViewById(R.id.diagram_frame);
		moveLayout.leftToRight = leftToRight;
		box = view.findViewById(R.id.diagram_box);
		//box.setBackgroundColor(0x22ff0000);
		graph = new Graph(Global.gc); // Create a diagram model
		forceDraw = true; // To be sure the diagram will be draw

		// Fade in animation
		ObjectAnimator alphaIn = ObjectAnimator.ofFloat(box, View.ALPHA, 1);
		alphaIn.setDuration(100);
		animator = new AnimatorSet();
		animator.play(alphaIn);

		return view;
	}

	// Individua il fulcro da cui partire, mostra eventuale bottone 'Crea la prima persona' oppure avvia il diagramma
	@Override
	public void onStart() {
		super.onStart();
		
		// Ragioni per cui bisogna proseguire, in particolare cose che sono cambiate
		if( forceDraw || (fulcrum != null && !fulcrum.getId().equals(Global.indi)) // TODO andrebbe testato
				|| (graph != null && graph.whichFamily != Global.familyNum) ) {
			forceDraw = false;
			box.removeAllViews();
			box.setAlpha(0);

			String[] ids = {Global.indi, Global.settings.getCurrentTree().root, U.trovaRadice(gc)};
			for( String id : ids ) {
				fulcrum = gc.getPerson(id);
				if (U.isConnector(fulcrum))
					continue;
				if( fulcrum != null )
					break;
			}
			// Empty diagram
			if( fulcrum == null ) {
				View button = LayoutInflater.from(getContext()).inflate(R.layout.diagram_button, null);
				button.findViewById(R.id.diagram_new).setOnClickListener(v ->
						startActivity(new Intent(getContext(), EditaIndividuo.class)
								.putExtra("idIndividuo", "TIZIO_NUOVO")
						)
				);
				new SuggestionBalloon(getContext(), button, R.string.new_person);
				if( !Global.settings.expert )
					((View)moveLayout.getParent()).findViewById(R.id.diagram_options).setVisibility(View.GONE);
			} else {
				Global.indi = fulcrum.getId(); // Casomai lo ribadisce
				graph.maxAncestors(Global.settings.diagram.ancestors)
						.maxGreatUncles(Global.settings.diagram.uncles)
						.displaySpouses(Global.settings.diagram.spouses)
						.maxDescendants(Global.settings.diagram.descendants)
						.maxSiblingsNephews(Global.settings.diagram.siblings)
						.maxUnclesCousins(Global.settings.diagram.cousins)
						.showFamily(Global.familyNum)
						.startFrom(fulcrum);
				drawDiagram();
			}
		}
	}

	// Put a view under the suggestion balloon
	class SuggestionBalloon extends ConstraintLayout {
		SuggestionBalloon(Context context, View childView, int suggestion) {
			super(context);
			View view = getLayoutInflater().inflate(R.layout.popup, this, true);
			box.addView(view);
			//setBackgroundColor(0x330066FF);
			LayoutParams nodeParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			nodeParams.topToBottom = R.id.popup_fumetto;
			nodeParams.startToStart = LayoutParams.PARENT_ID;
			nodeParams.endToEnd = LayoutParams.PARENT_ID;
			addView(childView, nodeParams);
			popup = view.findViewById(R.id.popup_fumetto);
			((TextView)popup.findViewById(R.id.popup_testo)).setText(suggestion);
			popup.setVisibility(INVISIBLE);
			popup.setOnTouchListener((v, e) -> {
				if( e.getAction() == MotionEvent.ACTION_DOWN ) {
					v.setVisibility(INVISIBLE);
					return true;
				}
				return false;
			});
			postDelayed(() -> {
				moveLayout.childWidth = box.getWidth();
				moveLayout.childHeight = box.getHeight();
				moveLayout.displayAll();
				animator.start();
			}, 100);
			popup.postDelayed(() -> popup.setVisibility(VISIBLE), 1000);
		}
		@Override
		public void invalidate() {
			if( printPDF ) {
				popup.setVisibility(GONE);
				if( glow != null ) glow.setVisibility(GONE);
			}
		}
	}

	// Diagram initialized the first time and clicking on a card
	void drawDiagram() {
		Log.d("Diagram", "drawDiagram");

		// Place various type of graphic nodes in the box taking them from the list of nodes
		for( PersonNode personNode : graph.getPersonNodes() ) {
			if( personNode.person.getId().equals(Global.indi) && !personNode.isFulcrumNode() )
				box.addView(new Asterisk(getContext(), personNode));
			else if (U.isConnector(personNode.person))
				box.addView(new Connector(getContext(), personNode));
			else if( personNode.mini )
				box.addView(new GraphicMiniCard(getContext(), personNode));
			else
				box.addView(new GraphicPerson(getContext(), personNode));
		}

		// Only one person in the diagram
		if( gc.getPeople().size() == 1 && gc.getFamilies().size() == 0 && !printPDF ) {

			// Put the card under the suggestion balloon
			View singleNode = box.getChildAt(0);
			box.removeView(singleNode);
			singleNode.setId(R.id.tag_fulcrum);
			ConstraintLayout popupLayout = new SuggestionBalloon(getContext(), singleNode, R.string.long_press_menu);

			// Add the glow to the fulcrum card
			if( fulcrumView != null ) {
				box.post(() -> {
					ConstraintLayout.LayoutParams glowParams = new ConstraintLayout.LayoutParams(
							singleNode.getWidth() + toPx(GLOW_SPACE * 2), singleNode.getHeight() + toPx(GLOW_SPACE * 2));
					glowParams.topToTop = R.id.tag_fulcrum;
					glowParams.bottomToBottom = R.id.tag_fulcrum;
					glowParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
					glowParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
					fulcrumView.metric.width = toDp(singleNode.getWidth());
					fulcrumView.metric.height = toDp(singleNode.getHeight());
					popupLayout.addView(new FulcrumGlow(getContext()), 0, glowParams);
				});
			}

		} else { // Two or more persons in the diagram or PDF print

			box.postDelayed( () -> {
				if (getActivity() == null)
					return;
				// Get the dimensions of each node converting from pixel to dip
				for( int i = 0; i < box.getChildCount(); i++ ) {
					View nodeView = box.getChildAt( i );
					if (nodeView instanceof GraphicMetric) {
						GraphicMetric graphic = (GraphicMetric)nodeView;
						// GraphicPerson can be larger because of VistaTesto, the child has the correct width
						graphic.metric.width = toDp(graphic.getChildAt(0).getWidth());
						graphic.metric.height = toDp(graphic.getChildAt(0).getHeight());
					}
				}
				graph.initNodes(); // Initialize nodes and lines

				// Add bond nodes
				for( Bond bond : graph.getBonds() ) {
					box.addView(new GraphicBond(getContext(), bond));
				}

				graph.placeNodes(); // Calculate first raw position

				// Add the lines
				lines = new Lines(getContext(), graph.getLines(), null);
				box.addView(lines, 0);
				backLines = new Lines(getContext(), graph.getBackLines(), new DashPathEffect(new float[]{toPx(4), toPx(4)}, 0));
				box.addView(backLines, 0);

				// Add the glow
				PersonNode fulcrumNode = (PersonNode)fulcrumView.metric;
				RelativeLayout.LayoutParams glowParams = new RelativeLayout.LayoutParams(
						toPx(fulcrumNode.width + GLOW_SPACE * 2), toPx(fulcrumNode.height + GLOW_SPACE * 2));
				glowParams.rightMargin = -toPx(GLOW_SPACE);
				glowParams.bottomMargin = -toPx(GLOW_SPACE);
				box.addView(new FulcrumGlow(getContext()), 0, glowParams);

				play = true;
				timer = new Timer();
				TimerTask task = new TimerTask() {
					@Override
					public void run() {
						if (getActivity() == null)
							return;
						getActivity().runOnUiThread(() -> {
							if( play ) {
								play = graph.playNodes(); // Check if there is still some nodes to move
								displaceDiagram();
							}
						});
						if( !play ) { // Animation is complete
							timer.cancel();
							// Sometimes lines need to be redrawn because MaxBitmap was not passed to graph
							if( graph.needMaxBitmap() ) {
								lines.postDelayed(() -> {
									graph.playNodes();
									lines.invalidate();
									backLines.invalidate();
								}, 500);
							}
						}
					}
				};
				moveLayout.virgin = true;
				timer.scheduleAtFixedRate(task, 0, 40); // 40 milliseconds = 25 fps

				animator.start();
			}, 100);
		}
	}

	// Update visible position of nodes and lines
	void displaceDiagram() {
		if( moveLayout.scaleDetector.isInProgress() )
			return;
		// Position of the nodes from dips to pixels
		for( int i = 0; i < box.getChildCount(); i++ ) {
			View nodeView = box.getChildAt(i);
			if( nodeView instanceof GraphicMetric ) {
				GraphicMetric graphicNode = (GraphicMetric)nodeView;
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)graphicNode.getLayoutParams();
				if( leftToRight ) params.leftMargin = toPx(graphicNode.metric.x);
				else params.rightMargin = toPx(graphicNode.metric.x);
				params.topMargin = toPx(graphicNode.metric.y);
			}
		}
		// The glow follows fulcrum
		RelativeLayout.LayoutParams glowParams = (RelativeLayout.LayoutParams)glow.getLayoutParams();
		if( leftToRight ) glowParams.leftMargin = toPx(fulcrumView.metric.x - GLOW_SPACE);
		else glowParams.rightMargin = toPx(fulcrumView.metric.x - GLOW_SPACE);
		glowParams.topMargin = toPx(fulcrumView.metric.y - GLOW_SPACE);

		moveLayout.childWidth = toPx(graph.getWidth()) + box.getPaddingStart() * 2;
		moveLayout.childHeight = toPx(graph.getHeight()) + box.getPaddingTop() * 2;

		// Update lines
		lines.invalidate();
		backLines.invalidate();

		// Pan to fulcrum
		if( moveLayout.virgin ) {
			float scale = moveLayout.minimumScale();
			float padding = box.getPaddingTop() * scale;
			moveLayout.panTo((int)(leftToRight ? toPx(fulcrumView.metric.centerX()) * scale - moveLayout.width / 2 + padding
							: moveLayout.width / 2 - toPx(fulcrumView.metric.centerX()) * scale - padding),
					(int)(toPx(fulcrumView.metric.centerY()) * scale - moveLayout.height / 2 + padding));
		} else {
			moveLayout.keepPositionResizing();
		}
		box.requestLayout();
	}

	// The glow around fulcrum card
	class FulcrumGlow extends View {
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		BlurMaskFilter bmf = new BlurMaskFilter(toPx(25), BlurMaskFilter.Blur.NORMAL);
		int extend = 5; // draw a rectangle a little bigger
		FulcrumGlow(Context context) {
			super(context == null ? Global.context : context);
			glow = this;
		}
		@Override
		protected void onDraw(Canvas canvas) {
			paint.setColor(getResources().getColor(R.color.evidenzia));
			paint.setMaskFilter(bmf);
			setLayerType(View.LAYER_TYPE_SOFTWARE, paint);
			canvas.drawRect(toPx(GLOW_SPACE - extend), toPx(GLOW_SPACE - extend),
					toPx(fulcrumView.metric.width + GLOW_SPACE + extend),
					toPx(fulcrumView.metric.height + GLOW_SPACE + extend), paint);
		}
		@Override
		public void invalidate() {
			if( printPDF ) {
				setVisibility(GONE);
			}
		}
	}

	// Node with one person or one bond
	abstract class GraphicMetric extends RelativeLayout {
		Metric metric;
		GraphicMetric(Context context, Metric metric) {
			super(context);
			this.metric = metric;
			setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
	}

	// Card of a person
	class GraphicPerson extends GraphicMetric {
		ImageView background;
		GraphicPerson(Context context, PersonNode personNode) {
			super(context, personNode);
			Person person = personNode.person;
			View view = getLayoutInflater().inflate(R.layout.diagram_card, this, true);
			View border = view.findViewById(R.id.card_border);
			if( Gender.isMale(person) )
				border.setBackgroundResource(R.drawable.casella_bordo_maschio);
			else if( Gender.isFemale(person) )
				border.setBackgroundResource(R.drawable.casella_bordo_femmina);
			background = view.findViewById(R.id.card_background);
			if( personNode.isFulcrumNode() ) {
				background.setBackgroundResource(R.drawable.casella_sfondo_evidente);
				fulcrumView = this;
			} else if( personNode.acquired ) {
				background.setBackgroundResource(R.drawable.casella_sfondo_sposo);
			}
			F.unaFoto( Global.gc, person, view.findViewById( R.id.card_photo ) );
			TextView vistaNome = view.findViewById(R.id.card_name);
			String nome = U.epiteto(person);
			if( nome.isEmpty() && view.findViewById(R.id.card_photo).getVisibility()==View.VISIBLE )
				vistaNome.setVisibility( View.GONE );
			else vistaNome.setText( nome );
			TextView vistaTitolo = view.findViewById(R.id.card_title);
			String titolo = U.titolo( person );
			if( titolo.isEmpty() ) vistaTitolo.setVisibility(View.GONE);
			else vistaTitolo.setText(titolo);
			TextView vistaDati = view.findViewById(R.id.card_data);
			String dati = U.twoDates(person, true);
			if( dati.isEmpty() ) vistaDati.setVisibility(View.GONE);
			else vistaDati.setText(dati);
			if( !U.isDead(person) )
				view.findViewById(R.id.card_mourn).setVisibility(View.GONE);
			registerForContextMenu(this);
			setOnClickListener( v -> {
				if( person.getId().equals(Global.indi) ) {
					Settings.Tree tree = settings.getCurrentTree();
					Log.d("DiagramClick", "Tapped on current focus: " + U.epiteto(person) + " (ID: " + person.getId() + ")");
					Log.d("DiagramClick", "Tree isForked: " + tree.isForked + ", Person isPrivate: " + U.isPrivate(person));
					
					// Check if this is the user's own forked tree
					boolean isOwnForkedTree = tree.isForked && isCurrentUserOwnerOfTree(tree);
					Log.d("DiagramClick", "Is own forked tree: " + isOwnForkedTree);
					
					// Allow viewing if: not forked, not private, or it's the user's own forked tree
					if (!(tree.isForked && U.isPrivate(person)) || isOwnForkedTree) {
						Log.d("DiagramClick", "Opening Individuo screen for: " + U.epiteto(person));
						Memoria.setPrimo(person);
						startActivity(new Intent(getContext(), Individuo.class));
					} else {
						Log.d("DiagramClick", "Blocked from opening Individuo - tree is forked, person is private, and not user's own tree");
					}
				} else {
					clickCard( person );
				}
			});
		}
		@Override
		public void invalidate() {
			// Change background color for PDF export
			if( printPDF && ((PersonNode)metric).acquired ) {
				background.setBackgroundResource(R.drawable.casella_sfondo_sposo_stampa);
			}
		}
	}

	// Marriage with eventual year and vertical line
	class GraphicBond extends GraphicMetric {
		View hearth;
		GraphicBond(Context context, Bond bond) {
			super(context, bond);
			RelativeLayout bondLayout = new RelativeLayout(context);
			//bondLayout.setBackgroundColor(0x44ff00ff);
			addView( bondLayout, new LayoutParams(toPx(bond.width), toPx(bond.height)) );
			FamilyNode familyNode = bond.familyNode;
			if( bond.marriageDate == null ) {
				hearth = new View(context);
				hearth.setBackgroundResource(R.drawable.diagram_hearth);
				int diameter = toPx(familyNode.mini ? MINI_HEARTH_DIAMETER : HEARTH_DIAMETER);
				LayoutParams hearthParams = new LayoutParams(diameter, diameter);
				hearthParams.topMargin = toPx(familyNode.centerRelY()) - diameter / 2;
				hearthParams.addRule(CENTER_HORIZONTAL);
				bondLayout.addView(hearth, hearthParams);
			} else {
				TextView year = new TextView( context );
				year.setBackgroundResource(R.drawable.diagram_year_oval);
				year.setGravity(Gravity.CENTER);
				year.setText(new Datatore(bond.marriageDate).writeDate(true));
				year.setTextSize(13f);
				LayoutParams yearParams = new LayoutParams(LayoutParams.MATCH_PARENT, toPx(MARRIAGE_HEIGHT));
				yearParams.topMargin = toPx(bond.centerRelY() - MARRIAGE_HEIGHT / 2);
				bondLayout.addView(year, yearParams);
			}
			setOnClickListener( view -> {
				Memoria.setPrimo( familyNode.spouseFamily );
				startActivity( new Intent( context, Famiglia.class ) );
			});
		}
		@Override
		public void invalidate() {
			if( printPDF && hearth != null ) {
				hearth.setBackgroundResource(R.drawable.diagram_hearth_print);
			}
		}
	}

	// Little ancestry or progeny card
	class GraphicMiniCard extends GraphicMetric {
		RelativeLayout layout;
		GraphicMiniCard(Context context, PersonNode personNode) {
			super(context, personNode);
			View miniCard = getLayoutInflater().inflate(R.layout.diagram_minicard, this, true);
			TextView miniCardText = miniCard.findViewById(R.id.minicard_text);
			miniCardText.setText(personNode.amount > 100 ? "100+" : String.valueOf(personNode.amount));
			Gender sex = Gender.getGender(personNode.person);
			if( sex == Gender.MALE )
				miniCardText.setBackgroundResource(R.drawable.casella_bordo_maschio);
			else if( sex == Gender.FEMALE )
				miniCardText.setBackgroundResource(R.drawable.casella_bordo_femmina);
			if( personNode.acquired ) {
				layout = miniCard.findViewById(R.id.minicard);
				layout.setBackgroundResource(R.drawable.casella_sfondo_sposo);
			}
			miniCard.setOnClickListener(view -> clickCard(personNode.person));
		}
		@Override
		public void invalidate() {
			if( printPDF && layout != null ) {
				layout.setBackgroundResource(R.drawable.casella_sfondo_sposo_stampa);
			}
		}
	}

	class Connector extends GraphicMetric {
		Connector(Context context, PersonNode personNode) {
			super(context, personNode);
			getLayoutInflater().inflate(R.layout.diagram_connector, this, true);
			setOnClickListener( v -> {
				// open subtree diagram
				openSubtree(personNode.person);
			});
		}
	}

	// Replacement for another person who is actually fulcrum
	class Asterisk extends GraphicMetric {
		Asterisk(Context context, PersonNode personNode) {
			super(context, personNode);
			getLayoutInflater().inflate(R.layout.diagram_asterisk, this, true);
			registerForContextMenu(this);
			setOnClickListener( v -> {
				Memoria.setPrimo(personNode.person);
				startActivity(new Intent(getContext(), Individuo.class));
			});
		}
	}

	// Generate the view of lines connecting the cards
	class Lines extends View {
		List<Set<Line>> lineGroups;
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		List<Path> paths = new ArrayList<>(); // Each path contains many lines
		//int[] colors = {Color.WHITE, Color.RED, Color.CYAN, Color.MAGENTA, Color.GREEN, Color.BLACK, Color.YELLOW, Color.BLUE};
		public Lines(Context context, List<Set<Line>> lineGroups, DashPathEffect effect) {
			super(context == null ? Global.context : context);
			//setBackgroundColor(0x330066ff);
			this.lineGroups = lineGroups;
			paint.setPathEffect(effect);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(STROKE);
		}
		@Override
		public void invalidate() {
			paint.setColor(getResources().getColor(printPDF ? R.color.lineeDiagrammaStampa : R.color.lineeDiagrammaSchermo));
			for( Path path : paths ){
				path.rewind();
			}
			float width = toPx(graph.getWidth());
			int pathNum = 0; // index of paths
			// Put the lines in one or more paths
			for( Set<Line> lineGroup : lineGroups ) {
				if( pathNum >= paths.size() )
					paths.add(new Path());
				Path path = paths.get(pathNum);
				for( Line line : lineGroup ) {
					float x1 = toPx(line.x1), y1 = toPx(line.y1), x2 = toPx(line.x2), y2 = toPx(line.y2);
					if( !leftToRight ) {
						x1 = width - x1;
						x2 = width - x2;
					}
					path.moveTo(x1, y1);
					if( line instanceof CurveLine ) {
						path.cubicTo(x1, y2, x2, y1, x2, y2);
					} else { // Horizontal or vertical line
						path.lineTo(x2, y2);
					}
				}
				pathNum++;
			}
			// Update this view size
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)getLayoutParams();
			params.width = toPx(graph.getWidth());
			params.height = toPx(graph.getHeight());
			requestLayout();
		}
		@Override
		protected void onDraw(Canvas canvas) {
			if( graph.needMaxBitmap() ) {
				int maxBitmapWidth = canvas.getMaximumBitmapWidth() // is 16384 on emulators, 4096 on my physical devices
						- STROKE * 4; // the space actually occupied by the line is a little bit larger
				int maxBitmapHeight = canvas.getMaximumBitmapHeight() - STROKE * 4;
				graph.setMaxBitmap((int)toDp(maxBitmapWidth), (int)toDp(maxBitmapHeight));
			}
			// Draw the paths
			//int p = 0;
			for( Path path : paths) {
				//paint.setColor(colors[p % colors.length]);
				canvas.drawPath(path, paint);
				//p++;
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if( timer != null ) {
			timer.cancel();
		}
	}

	private void clickCard(Person person) {
		Log.d("DiagramClick", "clickCard called for person: " + U.epiteto(person) + " (ID: " + person.getId() + ")");
		timer.cancel();
		selectParentFamily(person);
	}

	// Ask which family to display in the diagram if fulcrum has many parent families
	private void selectParentFamily(Person fulcrum) {
		Log.d("DiagramClick", "selectParentFamily called for: " + U.epiteto(fulcrum) + " (ID: " + fulcrum.getId() + ")");
		List<Family> families = fulcrum.getParentFamilies(gc);
		Log.d("DiagramClick", "Number of parent families: " + families.size());
		if( families.size() > 1 ) {
			new AlertDialog.Builder(getContext()).setTitle(R.string.which_family)
					.setItems(U.elencoFamiglie(families), (dialog, which) -> {
						completeSelect(fulcrum, which);
					}).show();
		} else {
			completeSelect(fulcrum, 0);
		}
	}
	// Complete above function
	private void completeSelect(Person fulcrum, int whichFamily) {
		Log.d("DiagramClick", "completeSelect called for: " + U.epiteto(fulcrum) + " (ID: " + fulcrum.getId() + "), family: " + whichFamily);
		Global.indi = fulcrum.getId();
		Global.familyNum = whichFamily;
		graph.showFamily(Global.familyNum);
		graph.startFrom(fulcrum);
		box.removeAllViews();
		box.setAlpha(0);
		drawDiagram();
	}

	private float toDp(float pixels) {
		return pixels / density;
	}

	private int toPx(float dips) {
		return (int) (dips * density + 0.5f);
	}

	// Generate the 2 family (as child and as partner) labels for contextual menu
	static String[] getFamilyLabels(Context context, Person person, Family family) {
		String[] labels = { null, null };
		List<Family> parentFams = person.getParentFamilies(gc);
		List<Family> spouseFams = person.getSpouseFamilies(gc);
		if( parentFams.size() > 0 )
			labels[0] = spouseFams.isEmpty() ? context.getString(R.string.family)
					: context.getString(R.string.family_as, Famiglia.getRole(person, null, Relation.CHILD, true).toLowerCase());
		if( family == null && spouseFams.size() == 1 )
			family = spouseFams.get(0);
		if( spouseFams.size() > 0 )
			labels[1] = parentFams.isEmpty() ? context.getString(R.string.family)
					: context.getString(R.string.family_as, Famiglia.getRole(person, family, Relation.PARTNER, true).toLowerCase());
		return labels;
	}

	private Person pers;
	private String idPersona;
	private Family parentFam; // Displayed family in which the person is child
	private Family spouseFam; // Selected family in which the person is spouse
	@Override
	public void onCreateContextMenu(ContextMenu menu, View vista, ContextMenu.ContextMenuInfo info) {
		PersonNode personNode = null;
		if( vista instanceof GraphicPerson )
			personNode = (PersonNode)((GraphicPerson)vista).metric;
		else if( vista instanceof Asterisk )
			personNode = (PersonNode)((Asterisk)vista).metric;
		pers = personNode.person;
		if( personNode.origin != null )
			parentFam = personNode.origin.spouseFamily;
		spouseFam = personNode.spouseFamily;
		idPersona = pers.getId();
		String[] familyLabels = getFamilyLabels(getContext(), pers, spouseFam);
		Settings.Tree tree = settings.getCurrentTree();
		if( idPersona.equals(Global.indi) && pers.getParentFamilies(gc).size() > 1 )
			menu.add(0, -1, 0, R.string.diagram);
		if( !idPersona.equals(Global.indi) )
			menu.add(0, 0, 0, R.string.card);
		if( familyLabels[0] != null )
			menu.add(0, 1, 0, familyLabels[0]);
		if( familyLabels[1] != null )
			menu.add(0, 2, 0, familyLabels[1]);
		menu.add(0, 3, 0, R.string.new_relative);
		if (Helper.isLogin(requireContext())) {
			if (tree.githubRepoFullName != null && !tree.githubRepoFullName.isEmpty() // has repository
					&& !tree.isForked
					&& !U.isConnector(pers)  // the person is not connector
					&& U.canBeConnector(pers, gc)
					)
				menu.add(0, 8, 0, R.string.assign_to_collaborators);
		}
		if( U.ciSonoIndividuiCollegabili(pers) ) {
			menu.add(0, 4, 0, R.string.link_person);
		}
		menu.add(0, 10, 0, R.string.relationship);
		if (!(tree.isForked && U.isPrivate(pers))) // dont allow edit if this repo is forked and person is private
		{
			menu.add(0, 5, 0, R.string.modify);
		}
		if( !pers.getParentFamilies(gc).isEmpty() || !pers.getSpouseFamilies(gc).isEmpty() )
			menu.add(0, 6, 0, R.string.unlink);

		menu.add(0, 7, 0, R.string.delete);

		// Hide import gedcom
		//menu.add(0, 9, 0, R.string.import_a_gedcom_file);

		if( popup != null )
			popup.setVisibility(View.INVISIBLE);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		CharSequence[] parenti = {getText(R.string.parent), getText(R.string.sibling),
				getText(R.string.partner), getText(R.string.child)};
		int id = item.getItemId();
		if( id == -1 ) { // Diagramma per fulcro figlio in più famiglie
			if( pers.getParentFamilies(gc).size() > 2 ) // Più di due famiglie
				selectParentFamily(pers);
			else // Due famiglie
				completeSelect(pers, Global.familyNum == 0 ? 1 : 0);
		} else if( id == 0 ) { // Apri scheda individuo
			Memoria.setPrimo(pers);
			startActivity(new Intent(getContext(), Individuo.class));
		} else if( id == 1 ) { // Famiglia come figlio
			if( idPersona.equals(Global.indi) ) { // Se è fulcro apre direttamente la famiglia
				Memoria.setPrimo(parentFam);
				startActivity(new Intent(getContext(), Famiglia.class));
			} else
				U.qualiGenitoriMostrare(getContext(), pers, 2);
		} else if( id == 2 ) { // Famiglia come coniuge
			U.qualiConiugiMostrare(getContext(), pers, null);
		} else if( id == 3 ) { // Collega persona nuova
			if (Global.settings.expert) {
				DialogFragment dialog = new NuovoParente(pers, parentFam, spouseFam, true, null);
				dialog.show(getActivity().getSupportFragmentManager(), "scegli");
			} else {
				new AlertDialog.Builder(getContext()).setItems(parenti, (dialog, quale) -> {
					Intent intento = new Intent(getContext(), EditaIndividuo.class);
					intento.putExtra("idIndividuo", idPersona);
					intento.putExtra("relazione", quale + 1);
					if (U.controllaMultiMatrimoni(intento, getContext(), null)) // aggiunge 'idFamiglia' o 'collocazione'
						return; // se perno è sposo in più famiglie, chiede a chi aggiungere un coniuge o un figlio
					startActivity(intento);
				}).show();
			}
		} else if (id == 8) {
			Helper.requireEmail(requireContext(),
					getString(R.string.set_email_for_commit),
					getString(R.string.OK), getString(R.string.cancel), email -> {
						assignToCollaborators(idPersona, email);
					});
		} else if (id == 9) {
			// import gedcom
			// show import gedcom screen
			if(Build.VERSION.SDK_INT <= 32){
				int perm = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
				if( perm == PackageManager.PERMISSION_DENIED )
					ActivityCompat.requestPermissions( requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1390 );
				else if( perm == PackageManager.PERMISSION_GRANTED )
					importaGedcom();
			}
			else{
				importaGedcom();
			}
		} else if( id == 4 ) { // Collega persona esistente
			if( Global.settings.expert ) {
				DialogFragment dialog = new NuovoParente(pers, parentFam, spouseFam, false, Diagram.this);
				dialog.show(getActivity().getSupportFragmentManager(), "scegli");
			} else {
				new AlertDialog.Builder(getContext()).setItems(parenti, (dialog, quale) -> {
					Intent intento = new Intent(getContext(), Principal.class);
					intento.putExtra("idIndividuo", idPersona);
					intento.putExtra("anagrafeScegliParente", true);
					intento.putExtra("relazione", quale + 1);
					if( U.controllaMultiMatrimoni(intento, getContext(), Diagram.this) )
						return;
					startActivityForResult(intento, 1401);
				}).show();
			}
		} else if( id == 5 ) { // Modifica
			if (U.isConnector(pers)) {
				Intent intento = new Intent(getContext(), EditConnectorActivity.class);
				intento.putExtra("idIndividuo", idPersona);
				startActivity(intento);
			} else {
				if(redirectEdit){
					Settings.Tree tree = settings.getCurrentTree();
					Memoria.setPrimo(pers);
					startActivity(new Intent(getContext(), Individuo.class));
				}
				else{
					Intent intento = new Intent(getContext(), EditaIndividuo.class);
					intento.putExtra("idIndividuo", idPersona);
					startActivity(intento);
				}
			}
		} else if( id == 6 ) { // Scollega
			unlink();
		} else if( id == 7 ) { // Elimina
			new AlertDialog.Builder(getContext()).setMessage(R.string.really_delete_person)
					.setPositiveButton(R.string.delete, (dialog, i) -> {
						Family[] famiglie = Anagrafe.eliminaPersona(getContext(), idPersona);
						ripristina();
						U.checkEmptyFamilies(getContext(), this::ripristina, false, famiglie);
					}).setNeutralButton(R.string.cancel, null).show();
		} else if (id == 10) { // Relationship
			Intent intento = new Intent(getContext(), Principal.class);
			intento.putExtra("idIndividuo", idPersona);
			intento.putExtra("showRelationshipInfo", true);
			startActivity(intento);
		} else
			return false;
		return true;
	}

	private void ripristina() {
		forceDraw = true;
		onStart();
	}

	void importaGedcom() {
		Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
		intent.setType( "application/*" );
		importGedcomActivityResultLauncher.launch(intent);
	}

	ActivityResultLauncher<Intent> importGedcomActivityResultLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result -> {
				try {
					Intent intent = result.getData();
					Uri uri = intent.getData();
					// Handle the returned Uri
					InputStream input = requireContext().getContentResolver().openInputStream(uri);
					Gedcom gc = new ModelParser().parseGedcom(input);
					if (gc.getHeader() == null) {
						Toast.makeText(requireContext(), R.string.invalid_gedcom, Toast.LENGTH_LONG).show();
						return;
					}
					gc.createIndexes(); // necessario per poi calcolare le generazioni

					// TODO: show list of all imported persons

				} catch (Exception ex) {
					FirebaseCrashlytics.getInstance().recordException(ex);
					ex.printStackTrace();
				}
			});

	private void assignToCollaborators(String idPersona, String email) {
		final ProgressDialog pd = new ProgressDialog(requireContext());
		pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pd.setTitle(R.string.cut_tree);
		pd.show();
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		Handler handler = new Handler(Looper.getMainLooper());
		executor.execute(() -> {
			Person person = gc.getPerson(idPersona);
			// split tree
			Settings.Tree tree = Global.settings.getCurrentTree();
			final TreeSplitter.SplitterResult result = TreeSplitter.split(gc, tree, person);

			// create local and new repo for sub tree
			int num = Global.settings.max() + 1;
			String subTreeRoot = "";
			for (int i = 0; i < result.T1.getPeople().size(); i++) {
				if (!U.isConnector(result.T1.getPeople().get(i))) {
					subTreeRoot = result.T1.getPeople().get(i).getId();
					break;
				}
			}

			// copy media from parent tree to sub tree
			File dirMediaSubTree = Helper.getDirMedia(getContext(), num);
			for(Person pSubTree: result.T1.getPeople()) {
				for (Media media: pSubTree.getAllMedia(result.T1)) {
					String filePath0 = F.percorsoMedia(tree.id, media);
					if (filePath0 != null) {
						File file0 = new File(filePath0);
						File file1 = new File(dirMediaSubTree, FilenameUtils.getName(media.getFile()));
						if (file0.exists()) {
							try {
								FileUtils.copyFile(file0, file1);
								file0.delete();
								Galleria.deleteMediaFileOnGithub(context, media);
							} catch (IOException e) {
								e.printStackTrace();
								FirebaseCrashlytics.getInstance().recordException(e);
							}
						}
					}
				}
			}

			File jsonSubtreeFile = new File(requireContext().getFilesDir(), num + ".json");
			Settings.Tree subTree = new Settings.Tree(num, tree.title + " [subtree]", null, result.personsT1, result.generationsT1, subTreeRoot, null, 0, "",
					null, null);
			JsonParser jp = new JsonParser();
			try {
				// create private peoples
				List<PrivatePerson> privatePersons = new ArrayList<>();
				for (Person _person : result.T1.getPeople()) {
					if (U.isPrivate(_person)) {
						PrivatePerson privatePerson = U.setPrivate(result.T1, _person);
						privatePersons.add(privatePerson);
					}
				}
				U.savePrivatePersons(num, privatePersons);
				FileUtils.writeStringToFile(jsonSubtreeFile, jp.toJson(result.T1), "UTF-8");
//				// put it back private people properties
//				for (PrivatePerson privatePerson: privatePersons) {
//					Person _person = result.T1.getPerson(privatePerson.personId);
//					if (_person != null) {
//						_person.setEventsFacts(privatePerson.eventFacts);
//						_person.setMedia(privatePerson.mediaList);
//					}
//				}
			} catch( Exception e ) {
				handler.post(() -> {
					Toast.makeText(requireContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				});
				return;
			}
			settings.aggiungi(subTree);
			settings.save();

			// create new repo for subtree
			final FamilyGemTreeInfoModel subTreeInfoModel = new FamilyGemTreeInfoModel(
					subTree.title, subTree.persons,subTree.generations,
					subTree.media, subTree.root, subTree.grade, subTree.createdAt, subTree.updatedAt
			);
			CreateRepoTask.execute(requireContext(),
					subTree.id, email, subTreeInfoModel, result.T1,
					(_id, _m) -> {
						String filePath = F.percorsoMedia(_id, _m);
						if (filePath != null)
							return new File(filePath);
						else
							return null;
					},
					() -> {
						pd.setMessage(getString(R.string.uploading));
						pd.show();
					}, deeplink -> {
						// it should set repoFullName in settings.json file
						subTree.githubRepoFullName = subTreeInfoModel.githubRepoFullName;
						Global.settings.save();
						// update connector with real github repo full name
						for (Person connector : result.connectors) {
							for (EventFact eventFact: connector.getEventsFacts()) {
								if (eventFact.getTag() != null && U.CONNECTOR_TAG.equals(eventFact.getTag())) {
									eventFact.setValue(subTree.githubRepoFullName);
								}
							}
						}
						// save current tree
						FamilyGemTreeInfoModel infoModel = new FamilyGemTreeInfoModel(
								tree.title,
								tree.persons,
								tree.generations,
								tree.media,
								tree.root,
								tree.grade,
								tree.createdAt,
								tree.updatedAt
						);
						U.salvaJson(gc, tree.id);
						SaveInfoFileTask.execute(requireContext(), tree.githubRepoFullName, email, tree.id, infoModel,  () -> {}, () -> {
							ripristina();
							pd.dismiss();
							// show screen "add collaborators"
							showScreenAddCollabarators(subTree);
						}, error -> {
							ripristina();
							pd.dismiss();
							Toast.makeText(Global.context, error, Toast.LENGTH_LONG).show();
						});
					}, error -> {
						ripristina();
						pd.dismiss();
						// show error message
						new AlertDialog.Builder(requireContext())
								.setTitle(R.string.find_errors)
								.setMessage(error)
								.setCancelable(false)
								.setPositiveButton(R.string.OK, (dialog, which) -> dialog.dismiss())
								.show();
					}
			);


		});
	}

	private void showScreenAddCollabarators(Settings.Tree subtree) {
		Intent intent = new Intent(getContext(), AddCollaboratorActivity.class);
		intent.putExtra("repoFullName", subtree.githubRepoFullName);
		startActivity(intent);
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data ) {
		if( resultCode == AppCompatActivity.RESULT_OK ) {
			// Aggiunge il parente che è stata scelto in Anagrafe. Adds the relative who was chosen in the registry
			if( requestCode == 1401 ) {
				Object[] modificati = EditaIndividuo.addRelative(
						data.getStringExtra("idIndividuo"), // corrisponde a 'idPersona', il quale però si annulla in caso di cambio di configurazione
						data.getStringExtra("idParente"),
						data.getStringExtra("idFamiglia"),
						data.getIntExtra("relazione", 0),
						data.getStringExtra("collocazione") );
				U.salvaJson( true, modificati );
			} // Export diagram to PDF
			else if( requestCode == 903 ) {
				// Stylize diagram for print
				printPDF = true;
				for( int i = 0; i < box.getChildCount(); i++ ) {
					box.getChildAt(i).invalidate();
				}
				fulcrumView.findViewById(R.id.card_background).setBackgroundResource(R.drawable.casella_sfondo_base);
				// Create PDF
				PdfDocument document = new PdfDocument();
				PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(box.getWidth(), box.getHeight(), 1).create();
				PdfDocument.Page page = document.startPage( pageInfo );
				box.draw( page.getCanvas() );
				document.finishPage(page);
				printPDF = false;
				// Write PDF
				Uri uri = data.getData();
				try {
					OutputStream out = getContext().getContentResolver().openOutputStream(uri, "wt");
					document.writeTo(out);
					out.flush();
					out.close();
				} catch( Exception e ) {
					Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
					return;
				}
				Toast.makeText(getContext(), R.string.pdf_exported_ok, Toast.LENGTH_LONG).show();
			} // Search person
			else if( requestCode == 904 ) {
				String selectedPersonId = data.getStringExtra("selectedPersonId");
				if( selectedPersonId != null ) {
					Global.indi = selectedPersonId;
					forceDraw = true;
					onStart(); // Trigger a full diagram refresh
				}
			}
		}
	}

	/**
	 * open the tree if it already exists locally, or
	 * subscribe if it is not subscribed yet, then open, or
	 * restore if it is subscribed but doesn’t exist yet locally, then open
	 * @param personConnector
	 */
	private void openSubtree(Person personConnector) {
		for (EventFact eventFact: personConnector.getEventsFacts()) {
			if (eventFact.getTag() != null && U.CONNECTOR_TAG.equals(eventFact.getTag())) {
				String githubRepoFullName = eventFact.getValue();
				// find treeId based on githubRepoFullName
				for (Settings.Tree tree: settings.trees) {
					if (tree.githubRepoFullName != null && tree.githubRepoFullName.equals(githubRepoFullName)) {
						openSubTree(tree.id);
						return;
					}
				}

				final ProgressDialog pd = new ProgressDialog(requireContext());
				pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				pd.setTitle(R.string.checking_data_on_server);
				pd.show();

				CheckAsCollaboratorTask.execute(getActivity(), githubRepoFullName, isCollaborator -> {
					if (isCollaborator) {
						downloadRepo(githubRepoFullName, pd);
					} else {
						getMyRepo(githubRepoFullName, pd);
					}
				}, error -> {
					// show error message
					new AlertDialog.Builder(getActivity())
							.setTitle(R.string.find_errors)
							.setMessage(error)
							.setCancelable(false)
							.setPositiveButton(R.string.OK, (dialog, which) -> {
								pd.dismiss();
								dialog.dismiss();

							})
							.show();
				});
				return;
			}
		}
	}

	private void openSubTree(int treeId) {
		// open principal activity
		if (!apriGedcom(treeId, true)) {
			return;
		}

		// open the tree if it already exists locally
		startActivity(new Intent(getActivity(), Principal.class));

		if (getActivity() == null || getActivity().isFinishing())
			return;
		getActivity().finish();
	}

	private void downloadRepo(String githubRepoFullName, final ProgressDialog pd) {
		int nextTreeId = Global.settings.max() + 1;
		RedownloadRepoTask.execute(getActivity(), githubRepoFullName, nextTreeId, infoModel -> {
			// add tree info and save settings.json
			Settings.Tree tree = new Settings.Tree(nextTreeId,
					infoModel.title,
					infoModel.filePath,
					infoModel.persons,
					infoModel.generations,
					infoModel.root,
					null,
					infoModel.grade,
					infoModel.githubRepoFullName,
					infoModel.createdAt,
					infoModel.updatedAt
			);
			tree.isForked = false;
			File dirMedia = Helper.getDirMedia(getContext(), nextTreeId);
			tree.dirs.add(dirMedia.getPath());
			Global.settings.aggiungi(tree);
			Global.settings.openTree = nextTreeId;
			Global.settings.save();

			if (getActivity() == null || getActivity().isFinishing())
				return;
			pd.dismiss();
			openSubTree(tree.id);
		}, error ->  {
			if (getActivity() == null || getActivity().isFinishing())
				return;

			new AlertDialog.Builder(getActivity())
					.setTitle(R.string.find_errors)
					.setMessage(error)
					.setCancelable(false)
					.setPositiveButton(R.string.OK, (dialog, which) -> {
						pd.dismiss();
						dialog.dismiss();
					}).show();
		});
	}


	// local repo can not be found, lets check it on server
	private void getMyRepo(String githubRepoFullName, final ProgressDialog pd) {

		pd.setTitle(R.string.download_shared_tree);
		pd.show();

		List<String> repoFullNames = U.getListOfCurrentRepoFullNames();
		GetMyReposTask.execute(getActivity(), repoFullNames, treeInfos -> {
			boolean existedOnServer = false;
			for (FamilyGemTreeInfoModel info: treeInfos) {
				if (info.githubRepoFullName.equals(githubRepoFullName)) {
					existedOnServer = true;
					break;
				}
			}
			int nextTreeId = Global.settings.max() + 1;
			if (existedOnServer) {
				// restore repo
				RedownloadRepoTask.execute(getActivity(), githubRepoFullName, nextTreeId, infoModel -> {
					// add tree info and save settings.json
					Settings.Tree tree = new Settings.Tree(nextTreeId,
							infoModel.title,
							infoModel.filePath,
							infoModel.persons,
							infoModel.generations,
							infoModel.root,
							null,
							infoModel.grade,
							infoModel.githubRepoFullName,
							infoModel.createdAt,
							infoModel.updatedAt
					);
					File dirMedia = Helper.getDirMedia(getContext(), nextTreeId);
					tree.dirs.add(dirMedia.getPath());
					tree.isForked = infoModel.isForked;
					Global.settings.aggiungi(tree);
					Global.settings.save();

					if (getActivity() == null || getActivity().isFinishing())
						return;

					openSubTree(tree.id);
				}, error ->  {
					if (getActivity() == null || getActivity().isFinishing())
						return;

					pd.dismiss();
					new AlertDialog.Builder(getActivity())
							.setTitle(R.string.find_errors)
							.setMessage(error)
							.setCancelable(false)
							.setPositiveButton(R.string.OK, (dialog, which) -> {
								dialog.dismiss();
							}).show();
				});
			} else {
				// fork repo
				ForkRepoTask.execute(getActivity(),
						githubRepoFullName, nextTreeId, () -> {
							// nothing yet
						}, infoModel  -> {
							// add tree info and save settings.json
							Settings.Tree tree = new Settings.Tree(nextTreeId,
									infoModel.title,
									infoModel.filePath,
									infoModel.persons,
									infoModel.generations,
									infoModel.root,
									null,
									infoModel.grade,
									infoModel.githubRepoFullName,
									infoModel.createdAt,
									infoModel.updatedAt
							);
							tree.isForked = true;
							tree.repoStatus = infoModel.repoStatus;
							tree.aheadBy = infoModel.aheadBy;
							tree.behindBy = infoModel.behindBy;
							tree.totalCommits = infoModel.totalCommits;
							Global.settings.aggiungi(tree);
							Global.settings.openTree = nextTreeId;
							Global.settings.save();

							if (getActivity() == null || getActivity().isFinishing())
								return;

							openSubTree(tree.id);
						}, error -> {
							if (getActivity() == null || getActivity().isFinishing())
								return;

							String errorMessage = error;
							if (error.equals("E001"))
								errorMessage = getString(R.string.error_cant_fork_repo_of_ourself);
							else if (error.equals("E404"))
								errorMessage = getString(R.string.error_shared_not_found);
							// show error message
							new AlertDialog.Builder(getActivity())
									.setTitle(R.string.find_errors)
									.setMessage(errorMessage)
									.setCancelable(false)
									.setPositiveButton(R.string.OK, (dialog, which) -> dialog.dismiss())
									.show();
						}
				);
			}
		}, error -> {
			if (getActivity() == null || getActivity().isFinishing())
				return;

			pd.dismiss();
			new AlertDialog.Builder(getActivity())
					.setTitle(R.string.find_errors)
					.setMessage(error)
					.setCancelable(false)
					.setPositiveButton(R.string.OK, (gDialog, gwhich) -> gDialog.dismiss())
					.show();
		});
	}

	private void unlink(){
		// If multiple link exist
		if(parentFam != null && spouseFam != null){
			String[] families = { getParentNames(parentFam), getSpuoseAndChildNames(spouseFam) };

			new AlertDialog.Builder(getContext()).setTitle(R.string.which_node)
				.setItems(families, (dialog, which) -> {
					if(which == 0){
						processUnlink(parentFam);
					}
					else if(which == 1) {
						processUnlink(spouseFam);
					}
				}).show();
		}
		else if( parentFam != null ) {
			processUnlink(parentFam);
		}
		else if( spouseFam != null ) {
			processUnlink(spouseFam);
		}
	}

	private void processUnlink(Family family){
		List<Family> modified = new ArrayList<>();
		Famiglia.disconnect(idPersona, family);
		modified.add(family);
		ripristina();
		Family[] modificateArr = modified.toArray(new Family[0]);
		U.checkEmptyFamilies(getContext(), this::ripristina, false, modificateArr);
		U.updateDate(pers);
		U.salvaJson(true, (Object[])modificateArr);
		displaceDiagram();
	}

	private String getParentNames(Family family){
		List<Person> people = new ArrayList<>();
		people.addAll(family.getHusbands(gc));
		people.addAll(family.getWives(gc));
		return getPersonsNames(people);
	}

	private String getSpuoseAndChildNames(Family family){
		List<Person> people = new ArrayList<>();
		people.addAll(family.getHusbands(gc));
		people.addAll(family.getWives(gc));
		people.addAll(family.getChildren(gc));
		people.removeIf(p -> p.getId() == idPersona);
		return getPersonsNames(people);
	}

	private String getPersonsNames(List<Person> people){
		List<String> names = new ArrayList<>();
		for(Person person: people){
			names.add(U.epiteto(person));
		}
		String nameJoined = String.join(", ", names);
		return  nameJoined;
	}

	/**
	 * Check if the current user owns the forked tree
	 */
	private boolean isCurrentUserOwnerOfTree(Settings.Tree tree) {
		try {
			Log.d("DiagramClick", "Checking tree ownership...");
			
			if (tree.githubRepoFullName == null || tree.githubRepoFullName.isEmpty()) {
				Log.d("DiagramClick", "No githubRepoFullName found");
				return false;
			}
			Log.d("DiagramClick", "Tree githubRepoFullName: " + tree.githubRepoFullName);
			
			// Get current user file
			File userFile = new File(getContext().getFilesDir(), "user");
			Log.d("DiagramClick", "User file exists: " + userFile.exists() + ", path: " + userFile.getAbsolutePath());
			if (!userFile.exists()) {
				return false;
			}
			
			// Use Helper from OAuth module to get user info
			com.familygem.restapi.models.User user = com.familygem.utility.Helper.getUser(userFile);
			Log.d("DiagramClick", "User object: " + (user != null ? "exists" : "null"));
			if (user == null || user.login == null) {
				Log.d("DiagramClick", "User login is null");
				return false;
			}
			Log.d("DiagramClick", "User login: " + user.login);
			
			// Extract owner from githubRepoFullName (format: "owner/repo")
			String[] parts = tree.githubRepoFullName.split("/");
			if (parts.length != 2) {
				Log.d("DiagramClick", "Invalid repo name format: " + tree.githubRepoFullName);
				return false;
			}
			
			String repoOwner = parts[0];
			boolean isOwner = user.login.equals(repoOwner);
			Log.d("DiagramClick", "Current user: " + user.login + ", Repo owner: " + repoOwner + ", Is owner: " + isOwner);
			return isOwner;
			
		} catch (Exception e) {
			Log.e("DiagramClick", "Error checking tree ownership", e);
			return false;
		}
	}
}
