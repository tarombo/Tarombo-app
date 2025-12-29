package app.familygem;

import static app.familygem.Trees.openGedcom;
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
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
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
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import app.familygem.diagram.*;

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
	private final boolean leftToRight = TextUtilsCompat
			.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_LTR;

	// Chart generation state
	private String tempChartType; // "descendants" or "ancestors"
	private String tempChartFormat; // "pdf", "jpeg", or "text"
	private String tempChartPersonId; // Root person ID
	private boolean chartTriggeredFromTrees = false; // Track if chart generation was triggered from Trees screen

	private static boolean redirectEdit = true;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		density = getResources().getDisplayMetrics().density;
		STROKE = toPx(2);

		getActivity().findViewById(R.id.toolbar).setVisibility(View.GONE); // Necessario in caso di backPressed dopo
																			// onActivityresult
		final View view = inflater.inflate(R.layout.diagram, container, false);
		view.findViewById(R.id.diagram_hamburger).setOnClickListener(v -> {
			DrawerLayout drawerLayout = getActivity().findViewById(R.id.drawerLayout);
			drawerLayout.openDrawer(GravityCompat.START);
		});
		view.findViewById(R.id.diagram_options).setOnClickListener(clickedView -> {
			PopupMenu popupMenu = new PopupMenu(getContext(), clickedView);
			Menu menu = popupMenu.getMenu();
			menu.add(0, 0, 0, R.string.diagram_settings);
			if (gc.getPeople().size() > 0) {
				menu.add(0, 1, 0, R.string.share_diagram);
				menu.add(0, 2, 0, R.string.export_diagram);
				menu.add(0, 3, 0, R.string.find_person);
			}
			popupMenu.show();
			popupMenu.setOnMenuItemClickListener(item -> {
				switch (item.getItemId()) {
					case 0: // Diagram settings
						startActivity(new Intent(getContext(), DiagramSettings.class));
						break;
					case 1: // Share diagram
						CharSequence[] shareFormats = { getText(R.string.pdf), getText(R.string.jpeg),
								getText(R.string.text) };
						new AlertDialog.Builder(getContext())
								.setTitle(R.string.choose_format)
								.setItems(shareFormats, (dialog, which) -> {
									switch (which) {
										case 0: // Share as PDF
											shareDiagramAsPDF();
											break;
										case 1: // Share as JPEG
											shareDiagramAsJPEG();
											break;
										case 2: // Share as Text
											shareDiagramAsText();
											break;
									}
								}).show();
						break;
					case 2: // Export diagram
						CharSequence[] exportFormats = { getText(R.string.pdf), getText(R.string.jpeg),
								getText(R.string.text) };
						new AlertDialog.Builder(getContext())
								.setTitle(R.string.choose_format)
								.setItems(exportFormats, (dialog, which) -> {
									switch (which) {
										case 0: // Export as PDF
											F.saveDocument(null, this, Global.settings.openTree, "application/pdf",
													"pdf", 903);
											break;
										case 1: // Export as JPEG
											F.saveDocument(null, this, Global.settings.openTree, "image/jpeg", "jpg",
													905);
											break;
										case 2: // Export as Text
											F.saveDocument(null, this, Global.settings.openTree, "text/plain", "txt",
													906);
											break;
									}
								}).show();
						break;
					case 3: // Find person
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
		// box.setBackgroundColor(0x22ff0000);
		graph = new Graph(Global.gc); // Create a diagram model
		forceDraw = true; // To be sure the diagram will be draw

		// Fade in animation
		ObjectAnimator alphaIn = ObjectAnimator.ofFloat(box, View.ALPHA, 1);
		alphaIn.setDuration(100);
		animator = new AnimatorSet();
		animator.play(alphaIn);

		return view;
	}

	public void enforcePrivateAccess(Settings.Tree tree, Person person, Runnable onGranted) {
		if (tree == null || person == null) {
			onGranted.run();
			return;
		}

		boolean isPrivateForked = tree.isForked && U.isPrivate(person);
		if (!isPrivateForked) {
			onGranted.run();
			return;
		}

		// determine base repo (source repo for forks, otherwise current)
		String baseRepoFullName = getBaseRepoFullName(tree);
		if (baseRepoFullName == null) {
			showAccessDeniedDialog(getString(R.string.not_collaborator_private_repo));
			return;
		}

		String ownerLogin = getRepoOwner(tree, baseRepoFullName);
		User user = Helper.getUser(new File(getContext().getFilesDir(), "user.json"));
		if (user != null && ownerLogin != null && ownerLogin.equals(user.login)) {
			onGranted.run();
			return;
		}

		String privateRepoFullName = baseRepoFullName + "-private";
		ProgressDialog pd = new ProgressDialog(getContext());
		pd.setMessage(getString(R.string.checking_collaborator_status));
		pd.setCancelable(false);
		pd.show();

		CheckAsCollaboratorTask.execute(
				getContext(),
				privateRepoFullName,
				isCollaborator -> {
					pd.dismiss();
					if (isCollaborator) {
						onGranted.run();
					} else {
						showAccessDeniedDialog(getString(R.string.not_collaborator_private_repo));
					}
				},
				error -> {
					pd.dismiss();
					showAccessDeniedDialog(error);
				});
	}

	private String getBaseRepoFullName(Settings.Tree tree) {
		if (tree.githubRepoFullName == null)
			return null;
		try {
			File repoFile = new File(getContext().getFilesDir(), tree.id + ".repo");
			Repo repo = Helper.getRepo(repoFile);
			if (repo != null && repo.source != null && repo.source.fullName != null) {
				return repo.source.fullName;
			}
		} catch (Exception ignored) {
		}
		return tree.githubRepoFullName;
	}

	private String getRepoOwner(Settings.Tree tree, String baseRepoFullName) {
		try {
			File repoFile = new File(getContext().getFilesDir(), tree.id + ".repo");
			Repo repo = Helper.getRepo(repoFile);
			if (repo != null) {
				if (repo.source != null && repo.source.owner != null && repo.source.owner.login != null)
					return repo.source.owner.login;
				if (repo.owner != null && repo.owner.login != null)
					return repo.owner.login;
			}
		} catch (Exception ignored) {
		}
		if (baseRepoFullName != null && baseRepoFullName.contains("/")) {
			return baseRepoFullName.substring(0, baseRepoFullName.indexOf("/"));
		}
		return null;
	}

	private void showAccessDeniedDialog(String message) {
		if (getContext() == null)
			return;
		new AlertDialog.Builder(requireContext())
				.setTitle(R.string.find_errors)
				.setMessage(message)
				.setPositiveButton(R.string.OK, (dialog, which) -> dialog.dismiss())
				.show();
	}

	// Individua il fulcro da cui partire, mostra eventuale bottone 'Crea la prima
	// persona' oppure avvia il diagramma
	@Override
	public void onStart() {
		super.onStart();

		// Ragioni per cui bisogna proseguire, in particolare cose che sono cambiate
		if (forceDraw || (fulcrum != null && !fulcrum.getId().equals(Global.indi)) // TODO andrebbe testato
				|| (graph != null && graph.whichFamily != Global.familyNum)) {
			forceDraw = false;
			box.removeAllViews();
			box.setAlpha(0);

			String[] ids = { Global.indi, Global.settings.getCurrentTree().root, U.findRoot(gc) };
			for (String id : ids) {
				fulcrum = gc.getPerson(id);
				if (U.isConnector(fulcrum))
					continue;
				if (fulcrum != null)
					break;
			}
			// Empty diagram
			if (fulcrum == null) {
				View button = LayoutInflater.from(getContext()).inflate(R.layout.diagram_button, null);
				button.findViewById(R.id.diagram_new)
						.setOnClickListener(v -> startActivity(new Intent(getContext(), EditaIndividuo.class)
								.putExtra("idIndividuo", "TIZIO_NUOVO")));
				new SuggestionBalloon(getContext(), button, R.string.new_person);
				if (!Global.settings.expert)
					((View) moveLayout.getParent()).findViewById(R.id.diagram_options).setVisibility(View.GONE);
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
			// setBackgroundColor(0x330066FF);
			LayoutParams nodeParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			nodeParams.topToBottom = R.id.popup_fumetto;
			nodeParams.startToStart = LayoutParams.PARENT_ID;
			nodeParams.endToEnd = LayoutParams.PARENT_ID;
			addView(childView, nodeParams);
			popup = view.findViewById(R.id.popup_fumetto);
			((TextView) popup.findViewById(R.id.popup_text)).setText(suggestion);
			popup.setVisibility(INVISIBLE);
			popup.setOnTouchListener((v, e) -> {
				if (e.getAction() == MotionEvent.ACTION_DOWN) {
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
			if (printPDF) {
				popup.setVisibility(GONE);
				if (glow != null)
					glow.setVisibility(GONE);
			}
		}
	}

	// Diagram initialized the first time and clicking on a card
	void drawDiagram() {
		Log.d("Diagram", "drawDiagram");

		// Place various type of graphic nodes in the box taking them from the list of
		// nodes
		for (PersonNode personNode : graph.getPersonNodes()) {
			if (personNode.person.getId().equals(Global.indi) && !personNode.isFulcrumNode())
				box.addView(new Asterisk(getContext(), personNode, this));
			else if (U.isConnector(personNode.person))
				box.addView(new Connector(getContext(), personNode, this));
			else if (personNode.mini)
				box.addView(new GraphicMiniCard(getContext(), personNode, this));
			else
				box.addView(new GraphicPerson(getContext(), personNode, this));
		}

		// Only one person in the diagram
		if (gc.getPeople().size() == 1 && gc.getFamilies().size() == 0 && !printPDF) {

			// Put the card under the suggestion balloon
			View singleNode = box.getChildAt(0);
			box.removeView(singleNode);
			singleNode.setId(R.id.tag_fulcrum);
			ConstraintLayout popupLayout = new SuggestionBalloon(getContext(), singleNode, R.string.long_press_menu);

			// Add the glow to the fulcrum card
			if (fulcrumView != null) {
				box.post(() -> {
					ConstraintLayout.LayoutParams glowParams = new ConstraintLayout.LayoutParams(
							singleNode.getWidth() + toPx(GLOW_SPACE * 2),
							singleNode.getHeight() + toPx(GLOW_SPACE * 2));
					glowParams.topToTop = R.id.tag_fulcrum;
					glowParams.bottomToBottom = R.id.tag_fulcrum;
					glowParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
					glowParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
					fulcrumView.metric.width = toDp(singleNode.getWidth());
					fulcrumView.metric.height = toDp(singleNode.getHeight());
					popupLayout.addView(new FulcrumGlow(getContext(), fulcrumView.metric), 0, glowParams);
				});
			}

		} else { // Two or more persons in the diagram or PDF print

			box.postDelayed(() -> {
				if (getActivity() == null)
					return;
				// Get the dimensions of each node converting from pixel to dip
				for (int i = 0; i < box.getChildCount(); i++) {
					View nodeView = box.getChildAt(i);
					if (nodeView instanceof GraphicMetric) {
						GraphicMetric graphic = (GraphicMetric) nodeView;
						// GraphicPerson can be larger because of VistaTesto, the child has the correct
						// width
						graphic.metric.width = toDp(graphic.getChildAt(0).getWidth());
						graphic.metric.height = toDp(graphic.getChildAt(0).getHeight());
					}
				}
				graph.initNodes(); // Initialize nodes and lines

				// Add bond nodes
				for (Bond bond : graph.getBonds()) {
					box.addView(new GraphicBond(getContext(), bond));
				}

				graph.placeNodes(); // Calculate first raw position

				// Add the lines
				lines = new Lines(getContext(), graph.getLines(), null);
				box.addView(lines, 0);
				backLines = new Lines(getContext(), graph.getBackLines(),
						new DashPathEffect(new float[] { toPx(4), toPx(4) }, 0));
				box.addView(backLines, 0);

				// Add the glow
				PersonNode fulcrumNode = (PersonNode) fulcrumView.metric;
				RelativeLayout.LayoutParams glowParams = new RelativeLayout.LayoutParams(
						toPx(fulcrumNode.width + GLOW_SPACE * 2), toPx(fulcrumNode.height + GLOW_SPACE * 2));
				glowParams.rightMargin = -toPx(GLOW_SPACE);
				glowParams.bottomMargin = -toPx(GLOW_SPACE);
				box.addView(new FulcrumGlow(getContext(), fulcrumView.metric), 0, glowParams);

				play = true;
				timer = new Timer();
				TimerTask task = new TimerTask() {
					@Override
					public void run() {
						if (getActivity() == null)
							return;
						getActivity().runOnUiThread(() -> {
							if (play) {
								play = graph.playNodes(); // Check if there is still some nodes to move
								displaceDiagram();
							}
						});
						if (!play) { // Animation is complete
							timer.cancel();
							// Sometimes lines need to be redrawn because MaxBitmap was not passed to graph
							if (graph.needMaxBitmap()) {
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
		if (moveLayout.scaleDetector.isInProgress())
			return;
		// Position of the nodes from dips to pixels
		for (int i = 0; i < box.getChildCount(); i++) {
			View nodeView = box.getChildAt(i);
			if (nodeView instanceof GraphicMetric) {
				GraphicMetric graphicNode = (GraphicMetric) nodeView;
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) graphicNode.getLayoutParams();
				if (leftToRight)
					params.leftMargin = toPx(graphicNode.metric.x);
				else
					params.rightMargin = toPx(graphicNode.metric.x);
				params.topMargin = toPx(graphicNode.metric.y);
			}
		}
		// The glow follows fulcrum
		RelativeLayout.LayoutParams glowParams = (RelativeLayout.LayoutParams) glow.getLayoutParams();
		if (leftToRight)
			glowParams.leftMargin = toPx(fulcrumView.metric.x - GLOW_SPACE);
		else
			glowParams.rightMargin = toPx(fulcrumView.metric.x - GLOW_SPACE);
		glowParams.topMargin = toPx(fulcrumView.metric.y - GLOW_SPACE);

		moveLayout.childWidth = toPx(graph.getWidth()) + box.getPaddingStart() * 2;
		moveLayout.childHeight = toPx(graph.getHeight()) + box.getPaddingTop() * 2;

		// Update lines
		lines.invalidate();
		backLines.invalidate();

		// Pan to fulcrum
		if (moveLayout.virgin) {
			float scale = moveLayout.minimumScale();
			float padding = box.getPaddingTop() * scale;
			moveLayout.panTo(
					(int) (leftToRight ? toPx(fulcrumView.metric.centerX()) * scale - moveLayout.width / 2 + padding
							: moveLayout.width / 2 - toPx(fulcrumView.metric.centerX()) * scale - padding),
					(int) (toPx(fulcrumView.metric.centerY()) * scale - moveLayout.height / 2 + padding));
		} else {
			moveLayout.keepPositionResizing();
		}
		box.requestLayout();
	}

	// Generate the view of lines connecting the cards
	class Lines extends View {
		List<Set<Line>> lineGroups;
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		List<Path> paths = new ArrayList<>(); // Each path contains many lines
		// int[] colors = {Color.WHITE, Color.RED, Color.CYAN, Color.MAGENTA,
		// Color.GREEN, Color.BLACK, Color.YELLOW, Color.BLUE};

		public Lines(Context context, List<Set<Line>> lineGroups, DashPathEffect effect) {
			super(context == null ? Global.context : context);
			// setBackgroundColor(0x330066ff);
			this.lineGroups = lineGroups;
			paint.setPathEffect(effect);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(STROKE);
		}

		@Override
		public void invalidate() {
			paint.setColor(
					getResources().getColor(printPDF ? R.color.lineeDiagrammaStampa : R.color.lineeDiagrammaSchermo));
			for (Path path : paths) {
				path.rewind();
			}
			float width = toPx(graph.getWidth());
			int pathNum = 0; // index of paths
			// Put the lines in one or more paths
			for (Set<Line> lineGroup : lineGroups) {
				if (pathNum >= paths.size())
					paths.add(new Path());
				Path path = paths.get(pathNum);
				for (Line line : lineGroup) {
					float x1 = toPx(line.x1), y1 = toPx(line.y1), x2 = toPx(line.x2), y2 = toPx(line.y2);
					if (!leftToRight) {
						x1 = width - x1;
						x2 = width - x2;
					}
					path.moveTo(x1, y1);
					if (line instanceof CurveLine) {
						path.cubicTo(x1, y2, x2, y1, x2, y2);
					} else { // Horizontal or vertical line
						path.lineTo(x2, y2);
					}
				}
				pathNum++;
			}
			// Update this view size
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
			params.width = toPx(graph.getWidth());
			params.height = toPx(graph.getHeight());
			requestLayout();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			if (graph.needMaxBitmap()) {
				int maxBitmapWidth = canvas.getMaximumBitmapWidth() // is 16384 on emulators, 4096 on my physical
																	// devices
						- STROKE * 4; // the space actually occupied by the line is a little bit larger
				int maxBitmapHeight = canvas.getMaximumBitmapHeight() - STROKE * 4;
				graph.setMaxBitmap((int) toDp(maxBitmapWidth), (int) toDp(maxBitmapHeight));
			}
			// Draw the paths
			// int p = 0;
			for (Path path : paths) {
				// paint.setColor(colors[p % colors.length]);
				canvas.drawPath(path, paint);
				// p++;
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (timer != null) {
			timer.cancel();
		}
	}

	public void clickCard(Person person) {
		Log.d("DiagramClick",
				"clickCard called for person: " + U.getPrincipalName(person) + " (ID: " + person.getId() + ")");
		timer.cancel();
		selectParentFamily(person);
	}

	// Ask which family to display in the diagram if fulcrum has many parent
	// families
	private void selectParentFamily(Person fulcrum) {
		Log.d("DiagramClick",
				"selectParentFamily called for: " + U.getPrincipalName(fulcrum) + " (ID: " + fulcrum.getId() + ")");
		List<Family> families = fulcrum.getParentFamilies(gc);
		Log.d("DiagramClick", "Number of parent families: " + families.size());
		if (families.size() > 1) {
			new AlertDialog.Builder(getContext()).setTitle(R.string.which_family)
					.setItems(U.listFamilies(families), (dialog, which) -> {
						completeSelect(fulcrum, which);
					}).show();
		} else {
			completeSelect(fulcrum, 0);
		}
	}

	// Complete above function
	private void completeSelect(Person fulcrum, int whichFamily) {
		Log.d("DiagramClick", "completeSelect called for: " + U.getPrincipalName(fulcrum) + " (ID: " + fulcrum.getId()
				+ "), family: " + whichFamily);
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
		if (parentFams.size() > 0)
			labels[0] = spouseFams.isEmpty() ? context.getString(R.string.family)
					: context.getString(R.string.family_as,
							Famiglia.getRole(person, null, Relation.CHILD, true).toLowerCase());
		if (family == null && spouseFams.size() == 1)
			family = spouseFams.get(0);
		if (spouseFams.size() > 0)
			labels[1] = parentFams.isEmpty() ? context.getString(R.string.family)
					: context.getString(R.string.family_as,
							Famiglia.getRole(person, family, Relation.PARTNER, true).toLowerCase());
		return labels;
	}

	private Person pers;
	private String personId;
	private Family parentFam; // Displayed family in which the person is child
	private Family spouseFam; // Selected family in which the person is spouse

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
		PersonNode personNode = null;
		if (view instanceof GraphicPerson)
			personNode = (PersonNode) ((GraphicPerson) view).metric;
		else if (view instanceof Asterisk)
			personNode = (PersonNode) ((Asterisk) view).metric;
		pers = personNode.person;
		if (personNode.origin != null)
			parentFam = personNode.origin.spouseFamily;
		spouseFam = personNode.spouseFamily;
		personId = pers.getId();
		String[] familyLabels = getFamilyLabels(getContext(), pers, spouseFam);
		Settings.Tree tree = settings.getCurrentTree();
		if (personId.equals(Global.indi) && pers.getParentFamilies(gc).size() > 1)
			menu.add(0, -1, 0, R.string.diagram);
		if (!personId.equals(Global.indi))
			menu.add(0, 0, 0, R.string.card);
		if (familyLabels[0] != null)
			menu.add(0, 1, 0, familyLabels[0]);
		if (familyLabels[1] != null)
			menu.add(0, 2, 0, familyLabels[1]);
		menu.add(0, 3, 0, R.string.new_relative);
		if (Helper.isLogin(requireContext())) {
			if (tree.githubRepoFullName != null && !tree.githubRepoFullName.isEmpty() // has repository
					&& !tree.isForked
					&& !U.isConnector(pers) // the person is not connector
					&& U.canBeConnector(pers, gc))
				menu.add(0, 8, 0, R.string.assign_to_collaborators);
		}
		if (U.areLinkablePersons(pers)) {
			menu.add(0, 4, 0, R.string.link_person);
		}
		menu.add(0, 10, 0, R.string.relationship);
		if (!(tree.isForked && U.isPrivate(pers))) // dont allow edit if this repo is forked and person is private
		{
			menu.add(0, 5, 0, R.string.modify);
		}
		if (!pers.getParentFamilies(gc).isEmpty() || !pers.getSpouseFamilies(gc).isEmpty())
			menu.add(0, 6, 0, R.string.unlink);

		menu.add(0, 7, 0, R.string.delete);

		// Hide import gedcom
		// menu.add(0, 9, 0, R.string.import_a_gedcom_file);

		if (popup != null)
			popup.setVisibility(View.INVISIBLE);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		CharSequence[] relatives = { getText(R.string.parent), getText(R.string.sibling),
				getText(R.string.partner), getText(R.string.child) };
		int id = item.getItemId();
		if (id == -1) { // Diagramma per fulcro figlio in più families
			if (pers.getParentFamilies(gc).size() > 2) // Più di due families
				selectParentFamily(pers);
			else // Due families
				completeSelect(pers, Global.familyNum == 0 ? 1 : 0);
		} else if (id == 0) { // Apri scheda individuo
			enforcePrivateAccess(settings.getCurrentTree(), pers, () -> {
				Memoria.setFirst(pers);
				startActivity(new Intent(getContext(), Individuo.class));
			});
		} else if (id == 1) { // Famiglia come figlio
			if (personId.equals(Global.indi)) { // Se è fulcro apre direttamente la famiglia
				Memoria.setFirst(parentFam);
				startActivity(new Intent(getContext(), Famiglia.class));
			} else
				U.qualiGenitoriMostrare(getContext(), pers, 2);
		} else if (id == 2) { // Famiglia come coniuge
			U.qualiConiugiMostrare(getContext(), pers, null);
		} else if (id == 3) { // Collega persona nuova
			if (Global.settings.expert) {
				DialogFragment dialog = new NuovoParente(pers, parentFam, spouseFam, true, null);
				dialog.show(getActivity().getSupportFragmentManager(), "scegli");
			} else {
				new AlertDialog.Builder(getContext()).setItems(relatives, (dialog, which) -> {
					Intent intent = new Intent(getContext(), EditaIndividuo.class);
					intent.putExtra("idIndividuo", personId);
					intent.putExtra("relazione", which + 1);
					if (U.controllaMultiMatrimoni(intent, getContext(), null)) // aggiunge 'idFamiglia' o
																				// 'collocazione'
						return; // se perno è sposo in più families, chiede a chi aggiungere un coniuge o un
								// figlio
					startActivity(intent);
				}).show();
			}
		} else if (id == 8) {
			Helper.requireEmail(requireContext(),
					getString(R.string.set_email_for_commit),
					getString(R.string.OK), getString(R.string.cancel), email -> {
						assignToCollaborators(personId, email);
					});
		} else if (id == 9) {
			// import gedcom
			// show import gedcom screen
			if (Build.VERSION.SDK_INT <= 32) {
				int perm = ContextCompat.checkSelfPermission(requireContext(),
						Manifest.permission.READ_EXTERNAL_STORAGE);
				if (perm == PackageManager.PERMISSION_DENIED)
					ActivityCompat.requestPermissions(requireActivity(),
							new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 1390);
				else if (perm == PackageManager.PERMISSION_GRANTED)
					importGedcom();
			} else {
				importGedcom();
			}
		} else if (id == 4) { // Collega persona esistente
			if (Global.settings.expert) {
				DialogFragment dialog = new NuovoParente(pers, parentFam, spouseFam, false, Diagram.this);
				dialog.show(getActivity().getSupportFragmentManager(), "scegli");
			} else {
				new AlertDialog.Builder(getContext()).setItems(relatives, (dialog, which) -> {
					Intent intent = new Intent(getContext(), Principal.class);
					intent.putExtra("idIndividuo", personId);
					intent.putExtra("anagrafeScegliParente", true);
					intent.putExtra("relazione", which + 1);
					if (U.controllaMultiMatrimoni(intent, getContext(), Diagram.this))
						return;
					startActivityForResult(intent, 1401);
				}).show();
			}
		} else if (id == 5) { // Modifica
			if (U.isConnector(pers)) {
				Intent intent = new Intent(getContext(), EditConnectorActivity.class);
				intent.putExtra("idIndividuo", personId);
				startActivity(intent);
			} else {
				if (redirectEdit) {
					Settings.Tree tree = settings.getCurrentTree();
					Memoria.setFirst(pers);
					startActivity(new Intent(getContext(), Individuo.class));
				} else {
					Intent intent = new Intent(getContext(), EditaIndividuo.class);
					intent.putExtra("idIndividuo", personId);
					startActivity(intent);
				}
			}
		} else if (id == 6) { // Scollega
			unlink();
		} else if (id == 7) { // Elimina
			new AlertDialog.Builder(getContext()).setMessage(R.string.really_delete_person)
					.setPositiveButton(R.string.delete, (dialog, i) -> {
						Family[] families = Anagrafe.eliminaPersona(getContext(), personId);
						restore();
						U.checkEmptyFamilies(getContext(), this::restore, false, families);
					}).setNeutralButton(R.string.cancel, null).show();
		} else if (id == 10) { // Relationship
			Intent intent = new Intent(getContext(), Principal.class);
			intent.putExtra("idIndividuo", personId);
			intent.putExtra("showRelationshipInfo", true);
			startActivity(intent);
		} else
			return false;
		return true;
	}

	private void restore() {
		forceDraw = true;
		onStart();
	}

	void importGedcom() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("application/*");
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

	private void assignToCollaborators(String personId, String email) {
		final ProgressDialog pd = new ProgressDialog(requireContext());
		pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pd.setTitle(R.string.cut_tree);
		pd.show();
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		Handler handler = new Handler(Looper.getMainLooper());
		executor.execute(() -> {
			Person person = gc.getPerson(personId);
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
			for (Person pSubTree : result.T1.getPeople()) {
				for (Media media : pSubTree.getAllMedia(result.T1)) {
					String filePath0 = F.getMediaPath(tree.id, media);
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
			Settings.Tree subTree = new Settings.Tree(num, tree.title + " [subtree]", null, result.personsT1,
					result.generationsT1, subTreeRoot, null, 0, "",
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
				// // put it back private people properties
				// for (PrivatePerson privatePerson: privatePersons) {
				// Person _person = result.T1.getPerson(privatePerson.personId);
				// if (_person != null) {
				// _person.setEventsFacts(privatePerson.eventFacts);
				// _person.setMedia(privatePerson.mediaList);
				// }
				// }
			} catch (Exception e) {
				handler.post(() -> {
					Toast.makeText(requireContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				});
				return;
			}
			settings.addTree(subTree);
			settings.save();

			// create new repo for subtree
			final FamilyGemTreeInfoModel subTreeInfoModel = new FamilyGemTreeInfoModel(
					subTree.title, subTree.persons, subTree.generations,
					subTree.media, subTree.root, subTree.grade, subTree.createdAt, subTree.updatedAt);
			CreateRepoTask.execute(requireContext(),
					subTree.id, email, subTreeInfoModel, result.T1,
					(_id, _m) -> {
						String filePath = F.getMediaPath(_id, _m);
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
							for (EventFact eventFact : connector.getEventsFacts()) {
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
								tree.updatedAt);
						U.saveJson(gc, tree.id);
						SaveInfoFileTask.execute(requireContext(), tree.githubRepoFullName, email, tree.id, infoModel,
								() -> {
								}, () -> {
									restore();
									pd.dismiss();
									// show screen "add collaborators"
									showScreenAddCollabarators(subTree);
								}, error -> {
									restore();
									pd.dismiss();
									Toast.makeText(Global.context, error, Toast.LENGTH_LONG).show();
								});
					}, error -> {
						restore();
						pd.dismiss();
						// show error message
						new AlertDialog.Builder(requireContext())
								.setTitle(R.string.find_errors)
								.setMessage(error)
								.setCancelable(false)
								.setPositiveButton(R.string.OK, (dialog, which) -> dialog.dismiss())
								.show();
					});

		});
	}

	private void showScreenAddCollabarators(Settings.Tree subtree) {
		Intent intent = new Intent(getContext(), AddCollaboratorActivity.class);
		intent.putExtra("repoFullName", subtree.githubRepoFullName);
		startActivity(intent);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == AppCompatActivity.RESULT_OK) {
			// Aggiunge il parente che è stata scelto in Anagrafe. Adds the relative who was
			// chosen in the registry
			if (requestCode == 1401) {
				Object[] modificati = EditaIndividuo.addRelative(
						data.getStringExtra("idIndividuo"), // corrisponde a 'personId', il which però si annulla in
															// caso di cambio di configurazione
						data.getStringExtra("idParente"),
						data.getStringExtra("idFamiglia"),
						data.getIntExtra("relazione", 0),
						data.getStringExtra("collocazione"));
				U.saveJson(true, modificati);
			} // Export diagram to PDF
			else if (requestCode == 903) {
				// Stylize diagram for print
				printPDF = true;
				for (int i = 0; i < box.getChildCount(); i++) {
					box.getChildAt(i).invalidate();
				}
				fulcrumView.findViewById(R.id.card_background).setBackgroundResource(R.drawable.casella_sfondo_base);
				// Create PDF
				PdfDocument document = new PdfDocument();
				PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(box.getWidth(), box.getHeight(), 1)
						.create();
				PdfDocument.Page page = document.startPage(pageInfo);
				box.draw(page.getCanvas());
				document.finishPage(page);
				printPDF = false;
				// Write PDF
				Uri uri = data.getData();
				try {
					OutputStream out = getContext().getContentResolver().openOutputStream(uri, "wt");
					document.writeTo(out);
					out.flush();
					out.close();
				} catch (Exception e) {
					Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
					return;
				}
				Toast.makeText(getContext(), R.string.pdf_exported_ok, Toast.LENGTH_LONG).show();
			} // Export diagram to JPEG
			else if (requestCode == 905) {
				// Stylize diagram for export
				printPDF = true;
				for (int i = 0; i < box.getChildCount(); i++) {
					box.getChildAt(i).invalidate();
				}
				fulcrumView.findViewById(R.id.card_background).setBackgroundResource(R.drawable.casella_sfondo_base);
				// Create Bitmap from diagram
				Bitmap bitmap = Bitmap.createBitmap(box.getWidth(), box.getHeight(), Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				box.draw(canvas);
				printPDF = false;
				// Write JPEG
				Uri uri = data.getData();
				try {
					OutputStream out = getContext().getContentResolver().openOutputStream(uri, "wt");
					bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
					out.flush();
					out.close();
					bitmap.recycle();
				} catch (Exception e) {
					Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
					return;
				}
				Toast.makeText(getContext(), R.string.jpeg_exported_ok, Toast.LENGTH_LONG).show();
			} // Export diagram to Text
			else if (requestCode == 906) {
				Uri uri = data.getData();
				try {
					String textContent = generateFamilyTreeText();
					OutputStream out = getContext().getContentResolver().openOutputStream(uri, "wt");
					out.write(textContent.getBytes("UTF-8"));
					out.flush();
					out.close();
					Toast.makeText(getContext(), R.string.text_exported_ok, Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				}
			} // Search person
			else if (requestCode == 904) {
				String selectedPersonId = data.getStringExtra("selectedPersonId");
				if (selectedPersonId != null) {
					Global.indi = selectedPersonId;
					forceDraw = true;
					onStart(); // Trigger a full diagram refresh
				}
			} // Chart generation - person selected
			else if (requestCode == 907) {
				tempChartPersonId = data.getStringExtra("selectedPersonId");
				if (tempChartPersonId != null) {
					// Determine MIME type and extension
					String mime, ext;
					switch (tempChartFormat) {
						case "pdf":
							mime = "application/pdf";
							ext = "pdf";
							break;
						case "jpeg":
							mime = "image/jpeg";
							ext = "jpg";
							break;
						case "text":
							mime = "text/plain";
							ext = "txt";
							break;
						default:
							return;
					}
					// Show save dialog
					F.saveDocument(null, this, Global.settings.openTree, mime, ext, 908);
				}
			} // Chart generation - file location selected
			else if (requestCode == 908) {
				Uri uri = data.getData();
				generateChart(uri);
			}
		}
	}

	/**
	 * Share diagram as PDF through Android share sheet
	 */
	private void shareDiagramAsPDF() {
		try {
			// Stylize diagram for export
			printPDF = true;
			for (int i = 0; i < box.getChildCount(); i++) {
				box.getChildAt(i).invalidate();
			}
			fulcrumView.findViewById(R.id.card_background).setBackgroundResource(R.drawable.casella_sfondo_base);

			// Create PDF
			PdfDocument document = new PdfDocument();
			PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(box.getWidth(), box.getHeight(), 1)
					.create();
			PdfDocument.Page page = document.startPage(pageInfo);
			box.draw(page.getCanvas());
			document.finishPage(page);

			// Write PDF to temp file
			File cacheDir = new File(getContext().getCacheDir(), "shared");
			cacheDir.mkdirs();
			String treeTitle = Global.settings.getTree(Global.settings.openTree).title.replaceAll("[$']", "_");
			File pdfFile = new File(cacheDir, treeTitle + ".pdf");

			FileOutputStream fos = new FileOutputStream(pdfFile);
			document.writeTo(fos);
			fos.flush();
			fos.close();
			document.close();

			// Get URI and share
			Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
					getContext(),
					getContext().getPackageName() + ".provider",
					pdfFile);

			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("application/pdf");
			shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
			shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(Intent.createChooser(shareIntent, getText(R.string.share_diagram)));

		} catch (Exception e) {
			Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		} finally {
			// Always reset state
			printPDF = false;
			// Restore normal UI
			for (int i = 0; i < box.getChildCount(); i++) {
				box.getChildAt(i).invalidate();
			}
		}
	}

	/**
	 * Share diagram as JPEG through Android share sheet
	 */
	private void shareDiagramAsJPEG() {
		Bitmap bitmap = null;
		try {
			// Stylize diagram for export
			printPDF = true;
			for (int i = 0; i < box.getChildCount(); i++) {
				box.getChildAt(i).invalidate();
			}
			fulcrumView.findViewById(R.id.card_background).setBackgroundResource(R.drawable.casella_sfondo_base);

			// Create Bitmap from diagram
			bitmap = Bitmap.createBitmap(box.getWidth(), box.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			box.draw(canvas);

			// Write JPEG to temp file
			File cacheDir = new File(getContext().getCacheDir(), "shared");
			cacheDir.mkdirs();
			String treeTitle = Global.settings.getTree(Global.settings.openTree).title.replaceAll("[$']", "_");
			File jpegFile = new File(cacheDir, treeTitle + ".jpg");

			FileOutputStream fos = new FileOutputStream(jpegFile);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
			fos.flush();
			fos.close();

			// Get URI and share
			Uri jpegUri = androidx.core.content.FileProvider.getUriForFile(
					getContext(),
					getContext().getPackageName() + ".provider",
					jpegFile);

			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("image/jpeg");
			shareIntent.putExtra(Intent.EXTRA_STREAM, jpegUri);
			shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(Intent.createChooser(shareIntent, getText(R.string.share_diagram)));

		} catch (Exception e) {
			Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		} finally {
			// Always reset state
			printPDF = false;
			// Clean up bitmap
			if (bitmap != null) {
				bitmap.recycle();
			}
			// Restore normal UI
			for (int i = 0; i < box.getChildCount(); i++) {
				box.getChildAt(i).invalidate();
			}
		}
	}

	/**
	 * open the tree if it already exists locally, or
	 * subscribe if it is not subscribed yet, then open, or
	 * restore if it is subscribed but doesn’t exist yet locally, then open
	 * 
	 * @param personConnector
	 */
	/**
	 * Share diagram as Text through Android share sheet
	 */
	private void shareDiagramAsText() {
		try {
			// Generate text representation
			String textContent = generateFamilyTreeText();

			// Write Text to temp file
			File cacheDir = new File(getContext().getCacheDir(), "shared");
			cacheDir.mkdirs();
			String treeTitle = Global.settings.getTree(Global.settings.openTree).title.replaceAll("[$']", "_");
			File textFile = new File(cacheDir, treeTitle + ".txt");

			FileOutputStream fos = new FileOutputStream(textFile);
			fos.write(textContent.getBytes("UTF-8"));
			fos.flush();
			fos.close();

			// Get URI and share
			Uri textUri = androidx.core.content.FileProvider.getUriForFile(
					getContext(),
					getContext().getPackageName() + ".provider",
					textFile);

			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_STREAM, textUri);
			shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(Intent.createChooser(shareIntent, getText(R.string.share_diagram)));

		} catch (Exception e) {
			Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Generate text representation of the family tree
	 */
	private String generateFamilyTreeText() {
		StringBuilder sb = new StringBuilder();
		Settings.Tree tree = Global.settings.getTree(Global.settings.openTree);
		sb.append(tree.title).append("\n");
		// Create equals line (Java 8 compatible)
		for (int i = 0; i < tree.title.length(); i++) {
			sb.append("=");
		}
		sb.append("\n\n");

		// Get persons from the current diagram view
		if (graph != null && graph.getPersonNodes() != null) {
			List<PersonNode> personNodes = graph.getPersonNodes();

			if (personNodes.isEmpty()) {
				sb.append(getString(R.string.no_persons)).append("\n");
			} else {
				// Find fulcrum person to display at the top
				Person fulcrumPerson = gc.getPerson(Global.indi);
				if (fulcrumPerson != null) {
					sb.append(getString(R.string.diagram_of)).append(": ");
					sb.append(U.getPrincipalName(fulcrumPerson)).append("\n\n");
				}

				// Organize persons by generation levels
				Map<Integer, List<PersonNode>> generationMap = new java.util.TreeMap<>();
				for (PersonNode node : personNodes) {
					// Skip mini cards (they represent collapsed branches)
					if (node.mini)
						continue;

					int generation = node.generation;
					if (!generationMap.containsKey(generation)) {
						generationMap.put(generation, new ArrayList<>());
					}
					generationMap.get(generation).add(node);
				}

				// Generate text by generation
				for (Map.Entry<Integer, List<PersonNode>> entry : generationMap.entrySet()) {
					int generation = entry.getKey();
					List<PersonNode> nodes = entry.getValue();

					// Generation label
					if (generation < 0) {
						sb.append("Generation ").append(-generation).append(" (Ancestors):\n");
					} else if (generation > 0) {
						sb.append("Generation +").append(generation).append(" (Descendants):\n");
					} else {
						sb.append("Current Generation:\n");
					}

					// List persons in this generation
					for (PersonNode node : nodes) {
						Person person = node.person;
						sb.append("  • ");
						sb.append(U.getPrincipalName(person));

						// Birth and death dates
						String dates = U.twoDates(person, false);
						if (dates != null && !dates.isEmpty()) {
							sb.append(" (").append(dates).append(")");
						}
						sb.append("\n");
					}
					sb.append("\n");
				}
			}
		} else {
			sb.append(getString(R.string.no_persons)).append("\n");
		}

		return sb.toString();
	}

	/**
	 * Show chart type selection dialog
	 */
	/**
	 * Show chart type selection dialog (public for external triggering)
	 */
	public void showChartTypeDialog(boolean fromTrees) {
		chartTriggeredFromTrees = fromTrees;
		CharSequence[] types = { getText(R.string.descendants), getText(R.string.ancestors) };
		new AlertDialog.Builder(getContext())
				.setTitle(R.string.chart_type)
				.setItems(types, (dialog, which) -> {
					tempChartType = which == 0 ? "descendants" : "ancestors";
					showChartFormatDialog();
				}).show();
	}

	/**
	 * Show chart format selection dialog
	 */
	private void showChartFormatDialog() {
		CharSequence[] formats = { getText(R.string.pdf), getText(R.string.jpeg), getText(R.string.text) };
		new AlertDialog.Builder(getContext())
				.setTitle(R.string.choose_format)
				.setItems(formats, (dialog, which) -> {
					switch (which) {
						case 0:
							tempChartFormat = "pdf";
							break;
						case 1:
							tempChartFormat = "jpeg";
							break;
						case 2:
							tempChartFormat = "text";
							break;
					}
					// Launch person selector
					Intent searchIntent = new Intent(getContext(), SearchPersonActivity.class);
					startActivityForResult(searchIntent, 907);
				}).show();
	}

	/**
	 * Generate chart based on selected type and format
	 */
	private void generateChart(Uri uri) {
		try {
			Person rootPerson = gc.getPerson(tempChartPersonId);
			if (rootPerson == null) {
				Toast.makeText(getContext(), R.string.person_not_found, Toast.LENGTH_SHORT).show();
				return;
			}

			if ("descendants".equals(tempChartType)) {
				if ("pdf".equals(tempChartFormat)) {
					generateDescendantsPDF(uri, rootPerson);
				} else if ("jpeg".equals(tempChartFormat)) {
					generateDescendantsJPEG(uri, rootPerson);
				} else if ("text".equals(tempChartFormat)) {
					generateDescendantsText(uri, rootPerson);
				}
			} else { // ancestors
				if ("pdf".equals(tempChartFormat)) {
					generateAncestorsPDF(uri, rootPerson);
				} else if ("jpeg".equals(tempChartFormat)) {
					generateAncestorsJPEG(uri, rootPerson);
				} else if ("text".equals(tempChartFormat)) {
					generateAncestorsText(uri, rootPerson);
				}
			}
		} catch (Exception e) {
			Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	/**
	 * Set default dimensions for graph nodes before layout calculation
	 * This is needed when creating graphs without UI views
	 */
	private void setNodeDimensions(Graph graph) {
		// Standard card dimensions in dips
		float cardWidth = 150f;
		float cardHeight = 60f;

		// Set dimensions for person nodes
		for (PersonNode node : graph.getPersonNodes()) {
			if (node.mini) {
				node.width = 20f; // Mini card diameter
				node.height = 20f;
			} else {
				node.width = cardWidth;
				node.height = cardHeight;
			}
		}

		// Set dimensions for family nodes
		for (Bond bond : graph.getBonds()) {
			if (bond.familyNode != null) {
				bond.familyNode.width = 10f; // Marriage symbol diameter
				bond.familyNode.height = 10f;
			}
		}
	}

	/**
	 * Generate descendants chart as PDF
	 */
	private void generateDescendantsPDF(Uri uri, Person rootPerson) throws IOException {
		// Temporarily switch to descendants view for this person
		String originalIndi = Global.indi;
		int originalFamilyNum = Global.familyNum;
		boolean wasAnimating = play;

		try {
			// Stop animation and set up for target person
			if (timer != null) {
				timer.cancel();
				play = false;
			}

			Global.indi = rootPerson.getId();
			Global.familyNum = 0;

			// Rebuild diagram with descendants settings
			int savedAncestors = Global.settings.diagram.ancestors;
			Global.settings.diagram.ancestors = 0; // No ancestors for descendants chart

			// Trigger diagram rebuild
			forceDraw = true; // Force redraw
			onStart(); // This will setup and draw the diagram from Global.indi

			// Wait for layout to complete
			box.postDelayed(() -> {
				try {
					// Stop animation and force complete layout
					if (timer != null) {
						timer.cancel();
						play = false;
					}

					// Play all animation frames to get final positions
					while (graph.playNodes()) {
					}
					displaceDiagram();

					// Measure and layout the view to content size
					int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
					int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
					box.measure(widthSpec, heightSpec);
					box.layout(0, 0, box.getMeasuredWidth(), box.getMeasuredHeight());

					// Stylize for PDF like Share does
					printPDF = true;
					for (int i = 0; i < box.getChildCount(); i++) {
						box.getChildAt(i).invalidate();
					}
					if (fulcrumView != null && fulcrumView.findViewById(R.id.card_background) != null) {
						fulcrumView.findViewById(R.id.card_background)
								.setBackgroundResource(R.drawable.casella_sfondo_base);
					}

					// Create PDF from measured view
					PdfDocument document = new PdfDocument();
					PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
							box.getMeasuredWidth(), box.getMeasuredHeight(), 1).create();
					PdfDocument.Page page = document.startPage(pageInfo);
					box.draw(page.getCanvas());
					document.finishPage(page);

					// Write to file
					OutputStream out = getContext().getContentResolver().openOutputStream(uri, "wt");
					document.writeTo(out);
					out.flush();
					out.close();
					document.close();

					// Restore settings
					Global.settings.diagram.ancestors = savedAncestors;
					printPDF = false;
					for (int i = 0; i < box.getChildCount(); i++) {
						box.getChildAt(i).invalidate();
					}

					// Restore original person
					Global.indi = originalIndi;
					Global.familyNum = originalFamilyNum;
					forceDraw = true;
					onStart();

					// Show success
					if (getActivity() != null) {
						getActivity().runOnUiThread(() -> {
							Toast.makeText(getContext(), R.string.chart_exported_ok, Toast.LENGTH_SHORT).show();
							if (chartTriggeredFromTrees) {
								getActivity().finish();
							}
						});
					}
				} catch (Exception e) {
					if (getActivity() != null) {
						getActivity().runOnUiThread(() -> {
							Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
						});
					}
				}
			}, 1000); // Wait 1 second for layout
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * Generate ancestors chart as PDF
	 */
	private void generateAncestorsPDF(Uri uri, Person rootPerson) throws IOException {
		String originalIndi = Global.indi;
		int originalFamilyNum = Global.familyNum;

		try {
			if (timer != null) {
				timer.cancel();
				play = false;
			}

			Global.indi = rootPerson.getId();
			Global.familyNum = 0;

			int savedDescendants = Global.settings.diagram.descendants;
			Global.settings.diagram.descendants = 0;

			forceDraw = true;
			onStart();

			box.postDelayed(() -> {
				try {
					if (timer != null) {
						timer.cancel();
						play = false;
					}

					while (graph.playNodes()) {
					}
					displaceDiagram();

					int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
					int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
					box.measure(widthSpec, heightSpec);
					box.layout(0, 0, box.getMeasuredWidth(), box.getMeasuredHeight());

					printPDF = true;
					for (int i = 0; i < box.getChildCount(); i++) {
						box.getChildAt(i).invalidate();
					}
					if (fulcrumView != null && fulcrumView.findViewById(R.id.card_background) != null) {
						fulcrumView.findViewById(R.id.card_background)
								.setBackgroundResource(R.drawable.casella_sfondo_base);
					}

					PdfDocument document = new PdfDocument();
					PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
							box.getMeasuredWidth(), box.getMeasuredHeight(), 1).create();
					PdfDocument.Page page = document.startPage(pageInfo);
					box.draw(page.getCanvas());
					document.finishPage(page);

					OutputStream out = getContext().getContentResolver().openOutputStream(uri, "wt");
					document.writeTo(out);
					out.flush();
					out.close();
					document.close();

					Global.settings.diagram.descendants = savedDescendants;
					printPDF = false;
					for (int i = 0; i < box.getChildCount(); i++) {
						box.getChildAt(i).invalidate();
					}

					Global.indi = originalIndi;
					Global.familyNum = originalFamilyNum;
					box.removeAllViews();
					drawDiagram();

					if (getActivity() != null) {
						getActivity().runOnUiThread(() -> {
							Toast.makeText(getContext(), R.string.chart_exported_ok, Toast.LENGTH_SHORT).show();
							if (chartTriggeredFromTrees) {
								getActivity().finish();
							}
						});
					}
				} catch (Exception e) {
					if (getActivity() != null) {
						getActivity().runOnUiThread(() -> {
							Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
						});
					}
				}
			}, 1000);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * Draw Graph to Canvas for PDF/JPEG generation
	 */
	private void drawGraphToCanvas(Canvas canvas, Graph chartGraph, float scale, float offsetX, float offsetY) {
		// Draw connecting lines first (so they appear behind cards)
		if (chartGraph.getBackLines() != null) {
			for (Set<Line> lineGroup : chartGraph.getBackLines()) {
				for (Line line : lineGroup) {
					drawLine(canvas, line, scale, offsetX, offsetY);
				}
			}
		}
		if (chartGraph.getLines() != null) {
			for (Set<Line> lineGroup : chartGraph.getLines()) {
				for (Line line : lineGroup) {
					drawLine(canvas, line, scale, offsetX, offsetY);
				}
			}
		}

		// Draw person nodes
		for (PersonNode personNode : chartGraph.getPersonNodes()) {
			if (personNode.mini) {
				drawMiniCard(canvas, personNode, scale, offsetX, offsetY);
			} else {
				drawPersonCard(canvas, personNode, scale, offsetX, offsetY);
			}
		}

		// Draw family nodes (marriage bonds)
		for (Bond bond : chartGraph.getBonds()) {
			if (bond.familyNode != null) {
				drawFamilyNode(canvas, bond.familyNode, scale, offsetX, offsetY);
			}
		}
	}

	/**
	 * Draw person card on canvas
	 */
	private void drawPersonCard(Canvas canvas, PersonNode node, float scale, float offsetX, float offsetY) {
		Paint bgPaint = new Paint();
		bgPaint.setColor(Color.WHITE);
		bgPaint.setStyle(Paint.Style.FILL);

		Paint borderPaint = new Paint();
		borderPaint.setColor(Color.GRAY);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(2 * scale);

		Paint textPaint = new Paint();
		textPaint.setTextSize(14 * scale);
		textPaint.setColor(Color.BLACK);
		textPaint.setAntiAlias(true);

		float cardWidth = 150 * scale;
		float cardHeight = 60 * scale;
		float x = node.x * scale + offsetX;
		float y = node.y * scale + offsetY;

		// Draw card background
		canvas.drawRect(x, y, x + cardWidth, y + cardHeight, bgPaint);
		canvas.drawRect(x, y, x + cardWidth, y + cardHeight, borderPaint);

		// Draw person name
		String name = U.getPrincipalName(node.person);
		if (name != null && !name.isEmpty()) {
			canvas.drawText(name, x + 10 * scale, y + 25 * scale, textPaint);
		}

		// Draw dates
		String dates = U.twoDates(node.person, false);
		if (dates != null && !dates.isEmpty()) {
			textPaint.setTextSize(12 * scale);
			canvas.drawText(dates, x + 10 * scale, y + 45 * scale, textPaint);
		}
	}

	/**
	 * Draw mini card (collapsed branch) on canvas
	 */
	private void drawMiniCard(Canvas canvas, PersonNode node, float scale, float offsetX, float offsetY) {
		Paint paint = new Paint();
		paint.setColor(Color.LTGRAY);
		paint.setStyle(Paint.Style.FILL);
		canvas.drawCircle(node.centerX() * scale + offsetX, node.centerY() * scale + offsetY, 10 * scale, paint);
	}

	/**
	 * Draw family node (marriage symbol) on canvas
	 */
	private void drawFamilyNode(Canvas canvas, FamilyNode node, float scale, float offsetX, float offsetY) {
		Paint paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStyle(Paint.Style.FILL);
		canvas.drawCircle(node.centerX() * scale + offsetX, node.centerY() * scale + offsetY, 5 * scale, paint);
	}

	/**
	 * Draw line on canvas
	 */
	private void drawLine(Canvas canvas, Line line, float scale, float offsetX, float offsetY) {
		Paint paint = new Paint();
		paint.setColor(Color.GRAY);
		paint.setStrokeWidth(2 * scale);
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);

		float x1 = line.x1 * scale + offsetX;
		float y1 = line.y1 * scale + offsetY;
		float x2 = line.x2 * scale + offsetX;
		float y2 = line.y2 * scale + offsetY;

		if (line instanceof CurveLine) {
			// Draw curved line using cubic Bezier (matching Lines class implementation)
			Path path = new Path();
			path.moveTo(x1, y1);
			path.cubicTo(x1, y2, x2, y1, x2, y2);
			canvas.drawPath(path, paint);
		} else {
			// Straight line
			canvas.drawLine(x1, y1, x2, y2, paint);
		}
	}

	/**
	 * Generate descendants chart as JPEG
	 */
	private void generateDescendantsJPEG(Uri uri, Person rootPerson) throws IOException {
		String originalIndi = Global.indi;
		int originalFamilyNum = Global.familyNum;

		try {
			if (timer != null) {
				timer.cancel();
				play = false;
			}

			Global.indi = rootPerson.getId();
			Global.familyNum = 0;

			int savedAncestors = Global.settings.diagram.ancestors;
			Global.settings.diagram.ancestors = 0;

			forceDraw = true;
			onStart();

			box.postDelayed(() -> {
				Bitmap bitmap = null;
				try {
					if (timer != null) {
						timer.cancel();
						play = false;
					}

					while (graph.playNodes()) {
					}
					displaceDiagram();

					int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
					int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
					box.measure(widthSpec, heightSpec);
					box.layout(0, 0, box.getMeasuredWidth(), box.getMeasuredHeight());

					printPDF = true;
					for (int i = 0; i < box.getChildCount(); i++) {
						box.getChildAt(i).invalidate();
					}
					if (fulcrumView != null && fulcrumView.findViewById(R.id.card_background) != null) {
						fulcrumView.findViewById(R.id.card_background)
								.setBackgroundResource(R.drawable.casella_sfondo_base);
					}

					bitmap = Bitmap.createBitmap(box.getMeasuredWidth(), box.getMeasuredHeight(),
							Bitmap.Config.ARGB_8888);
					Canvas canvas = new Canvas(bitmap);
					box.draw(canvas);

					OutputStream out = getContext().getContentResolver().openOutputStream(uri, "wt");
					bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
					out.flush();
					out.close();

					Global.settings.diagram.ancestors = savedAncestors;
					printPDF = false;
					if (bitmap != null)
						bitmap.recycle();
					for (int i = 0; i < box.getChildCount(); i++) {
						box.getChildAt(i).invalidate();
					}

					Global.indi = originalIndi;
					Global.familyNum = originalFamilyNum;
					box.removeAllViews();
					drawDiagram();

					if (getActivity() != null) {
						getActivity().runOnUiThread(() -> {
							Toast.makeText(getContext(), R.string.chart_exported_ok, Toast.LENGTH_SHORT).show();
							if (chartTriggeredFromTrees) {
								getActivity().finish();
							}
						});
					}
				} catch (Exception e) {
					if (bitmap != null)
						bitmap.recycle();
					if (getActivity() != null) {
						getActivity().runOnUiThread(() -> {
							Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
						});
					}
				}
			}, 1000);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * Generate ancestors chart as JPEG
	 */
	private void generateAncestorsJPEG(Uri uri, Person rootPerson) throws IOException {
		String originalIndi = Global.indi;
		int originalFamilyNum = Global.familyNum;

		try {
			if (timer != null) {
				timer.cancel();
				play = false;
			}

			Global.indi = rootPerson.getId();
			Global.familyNum = 0;

			int savedDescendants = Global.settings.diagram.descendants;
			Global.settings.diagram.descendants = 0;

			forceDraw = true;
			onStart();

			box.postDelayed(() -> {
				Bitmap bitmap = null;
				try {
					if (timer != null) {
						timer.cancel();
						play = false;
					}

					while (graph.playNodes()) {
					}
					displaceDiagram();

					int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
					int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
					box.measure(widthSpec, heightSpec);
					box.layout(0, 0, box.getMeasuredWidth(), box.getMeasuredHeight());

					printPDF = true;
					for (int i = 0; i < box.getChildCount(); i++) {
						box.getChildAt(i).invalidate();
					}
					if (fulcrumView != null && fulcrumView.findViewById(R.id.card_background) != null) {
						fulcrumView.findViewById(R.id.card_background)
								.setBackgroundResource(R.drawable.casella_sfondo_base);
					}

					bitmap = Bitmap.createBitmap(box.getMeasuredWidth(), box.getMeasuredHeight(),
							Bitmap.Config.ARGB_8888);
					Canvas canvas = new Canvas(bitmap);
					box.draw(canvas);

					OutputStream out = getContext().getContentResolver().openOutputStream(uri, "wt");
					bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
					out.flush();
					out.close();

					Global.settings.diagram.descendants = savedDescendants;
					printPDF = false;
					if (bitmap != null)
						bitmap.recycle();
					for (int i = 0; i < box.getChildCount(); i++) {
						box.getChildAt(i).invalidate();
					}

					Global.indi = originalIndi;
					Global.familyNum = originalFamilyNum;
					box.removeAllViews();
					drawDiagram();

					if (getActivity() != null) {
						getActivity().runOnUiThread(() -> {
							Toast.makeText(getContext(), R.string.chart_exported_ok, Toast.LENGTH_SHORT).show();
							if (chartTriggeredFromTrees) {
								getActivity().finish();
							}
						});
					}
				} catch (Exception e) {
					if (bitmap != null)
						bitmap.recycle();
					if (getActivity() != null) {
						getActivity().runOnUiThread(() -> {
							Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
						});
					}
				}
			}, 1000);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * Generate descendants chart as Text
	 */
	private void generateDescendantsText(Uri uri, Person rootPerson) throws IOException {
		StringBuilder sb = new StringBuilder();

		// Header
		String rootName = U.getPrincipalName(rootPerson);
		sb.append("Descendants of ").append(rootName).append("\n");
		sb.append("-------------------------------------------\n");

		// Build tree recursively starting with empty prefixes for root person
		buildDescendantsTree(sb, rootPerson, 1, "", "");

		sb.append("-------------------------------------------\n");

		// Write to file
		OutputStream out = getContext().getContentResolver().openOutputStream(uri, "wt");
		out.write(sb.toString().getBytes("UTF-8"));
		out.flush();
		out.close();
	}

	/**
	 * Get death place from person's events
	 */
	private String getDeathPlace(Person person) {
		if (person == null)
			return null;
		for (EventFact fact : person.getEventsFacts()) {
			if (fact.getTag() != null && fact.getTag().equals("DEAT") && fact.getPlace() != null) {
				return fact.getPlace();
			}
		}
		return null;
	}

	/**
	 * Recursively build descendants tree in vertical format
	 * 
	 * @param sb                 StringBuilder to append output
	 * @param person             Current person to process
	 * @param generation         Generation number
	 * @param linePrefix         Prefix for this person's line (includes |-- for
	 *                           children)
	 * @param continuationPrefix Prefix for descendants of this person
	 */
	private void buildDescendantsTree(StringBuilder sb, Person person, int generation, String linePrefix,
			String continuationPrefix) {
		if (person == null)
			return;

		// Get person info
		String name = U.getPrincipalName(person);
		String dates = U.twoDates(person, false);
		String deathPlace = getDeathPlace(person);

		// Build person line with linePrefix
		StringBuilder personLine = new StringBuilder();
		personLine.append(linePrefix).append(generation).append("-").append(name);
		if (dates != null && !dates.isEmpty()) {
			personLine.append(" ").append(dates);
		}
		if (deathPlace != null && !deathPlace.isEmpty()) {
			personLine.append(", ").append(deathPlace);
		}

		// Write person line, wrapping long lines
		writeWrappedLine(sb, personLine.toString(), linePrefix.length());

		// Get families where this person is a parent
		List<Family> families = person.getSpouseFamilies(gc);
		if (families == null || families.isEmpty()) {
			return;
		}

		// Process each family
		for (int famIdx = 0; famIdx < families.size(); famIdx++) {
			Family family = families.get(famIdx);

			// Get spouses - collect both husbands and wives, excluding current person
			List<Person> spouses = new ArrayList<>();
			for (Person husband : family.getHusbands(gc)) {
				if (!husband.getId().equals(person.getId())) {
					spouses.add(husband);
				}
			}
			for (Person wife : family.getWives(gc)) {
				if (!wife.getId().equals(person.getId())) {
					spouses.add(wife);
				}
			}

			// Write spouse lines
			// For root person (generation 1), use " + " (2 spaces)
			// For descendants, use continuationPrefix + spaces to align with person's name
			for (Person spouse : spouses) {
				String spouseName = U.getPrincipalName(spouse);
				String spouseDates = U.twoDates(spouse, false);
				String spouseDeathPlace = getDeathPlace(spouse);

				StringBuilder spouseLine = new StringBuilder();
				if (generation == 1) {
					// Root person: '+' aligned under '1'
					spouseLine.append(" + ").append(spouseName);
				} else {
					// Descendants: '+' aligned under generation digit
					// Person line has: continuationPrefix + "|--" + generation + "-" + name
					// Spouse line needs: continuationPrefix + "+ " to align '+' with generation
					// digit
					spouseLine.append(continuationPrefix).append("+ ").append(spouseName);
				}
				if (spouseDates != null && !spouseDates.isEmpty()) {
					spouseLine.append(" ").append(spouseDates);
				}
				if (spouseDeathPlace != null && !spouseDeathPlace.isEmpty()) {
					spouseLine.append(", ").append(spouseDeathPlace);
				}

				int baseIndent = (generation == 1) ? 3 : (continuationPrefix.length() + 2);
				writeWrappedLine(sb, spouseLine.toString(), baseIndent);
			}

			// Get children
			List<Person> children = family.getChildren(gc);
			if (children != null && !children.isEmpty()) {
				for (int childIdx = 0; childIdx < children.size(); childIdx++) {
					Person child = children.get(childIdx);
					boolean isLastChild = (childIdx == children.size() - 1) && (famIdx == families.size() - 1);

					// Build child's line prefix and continuation prefix for its descendants
					String childLinePrefix = continuationPrefix + "|--";
					String childContinuationPrefix = continuationPrefix + (isLastChild ? "   " : "|  ");

					// Recursively build child's descendants with line prefix (includes |--) and
					// continuation prefix
					buildDescendantsTree(sb, child, generation + 1, childLinePrefix, childContinuationPrefix);
				}
			}
		}
	}

	/**
	 * Write a line with wrapping for long content
	 * 
	 * @param sb         StringBuilder to append to
	 * @param line       The line content
	 * @param baseIndent The base indentation level
	 */
	private void writeWrappedLine(StringBuilder sb, String line, int baseIndent) {
		final int MAX_LINE_LENGTH = 70;

		if (line.length() <= MAX_LINE_LENGTH) {
			sb.append(line).append("\n");
			return;
		}

		// Find a good break point (comma, space) after 70 chars
		int breakPoint = -1;
		for (int i = MAX_LINE_LENGTH; i < line.length() && i < MAX_LINE_LENGTH + 20; i++) {
			if (line.charAt(i) == ',' && i + 1 < line.length()) {
				breakPoint = i + 1; // Include comma and space
				break;
			}
		}

		if (breakPoint > 0) {
			// Write first part
			sb.append(line.substring(0, breakPoint).trim()).append("\n");

			// Write continuation with indentation
			String indent = "";
			for (int i = 0; i < baseIndent + 4; i++) {
				indent += " ";
			}
			sb.append(indent).append(line.substring(breakPoint).trim()).append("\n");
		} else {
			// No good break point, just write as is
			sb.append(line).append("\n");
		}
	}

	/**
	 * Generate ancestors chart as Text
	 */
	private void generateAncestorsText(Uri uri, Person rootPerson) throws IOException {
		StringBuilder sb = new StringBuilder();

		// Header
		String rootName = U.getPrincipalName(rootPerson);
		sb.append("Ancestors of ").append(rootName).append("\n");
		sb.append("-------------------------------------------\n");

		// Build tree recursively starting with empty prefixes
		buildAncestorsTree(sb, rootPerson, 1, "", "");

		sb.append("-------------------------------------------\n");

		// Write to file
		OutputStream out = getContext().getContentResolver().openOutputStream(uri, "wt");
		out.write(sb.toString().getBytes("UTF-8"));
		out.flush();
		out.close();
	}

	/**
	 * Recursively build ancestors tree in vertical format
	 * 
	 * @param sb                 StringBuilder to append output
	 * @param person             Current person to process
	 * @param generation         Generation number
	 * @param linePrefix         Prefix for this person's line (includes |-- for
	 *                           parents)
	 * @param continuationPrefix Prefix for ancestors of this person
	 */
	private void buildAncestorsTree(StringBuilder sb, Person person, int generation, String linePrefix,
			String continuationPrefix) {
		if (person == null)
			return;

		// Get person info
		String name = U.getPrincipalName(person);
		String dates = U.twoDates(person, false);
		String deathPlace = getDeathPlace(person);

		// Build person line with linePrefix
		StringBuilder personLine = new StringBuilder();
		personLine.append(linePrefix).append(generation).append("-").append(name);
		if (dates != null && !dates.isEmpty()) {
			personLine.append(" ").append(dates);
		}
		if (deathPlace != null && !deathPlace.isEmpty()) {
			personLine.append(", ").append(deathPlace);
		}

		// Write person line, wrapping long lines
		writeWrappedLine(sb, personLine.toString(), linePrefix.length());

		// Get parent families
		List<Family> parentFamilies = person.getParentFamilies(gc);
		if (parentFamilies == null || parentFamilies.isEmpty()) {
			return;
		}

		// Process each parent family
		for (int famIdx = 0; famIdx < parentFamilies.size(); famIdx++) {
			Family family = parentFamilies.get(famIdx);

			// Get parents - collect both fathers and mothers
			List<Person> parents = new ArrayList<>();
			for (Person father : family.getHusbands(gc)) {
				parents.add(father);
			}
			for (Person mother : family.getWives(gc)) {
				parents.add(mother);
			}

			// Process each parent
			for (int parentIdx = 0; parentIdx < parents.size(); parentIdx++) {
				Person parent = parents.get(parentIdx);
				boolean isLastParent = (parentIdx == parents.size() - 1) && (famIdx == parentFamilies.size() - 1);

				// Build parent's line prefix and continuation prefix for its ancestors
				String parentLinePrefix = continuationPrefix + "|--";
				String parentContinuationPrefix = continuationPrefix + (isLastParent ? "   " : "|  ");

				// Recursively build parent's ancestors with line prefix and continuation prefix
				buildAncestorsTree(sb, parent, generation + 1, parentLinePrefix, parentContinuationPrefix);
			}
		}
	}

	public void openSubtree(Person personConnector) {
		for (EventFact eventFact : personConnector.getEventsFacts()) {
			if (eventFact.getTag() != null && U.CONNECTOR_TAG.equals(eventFact.getTag())) {
				String githubRepoFullName = eventFact.getValue();
				// find treeId based on githubRepoFullName
				for (Settings.Tree tree : settings.trees) {
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
		if (!openGedcom(treeId, true)) {
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
					infoModel.updatedAt);
			tree.isForked = false;
			File dirMedia = Helper.getDirMedia(getContext(), nextTreeId);
			tree.dirs.add(dirMedia.getPath());
			Global.settings.addTree(tree);
			Global.settings.openTree = nextTreeId;
			Global.settings.save();

			if (getActivity() == null || getActivity().isFinishing())
				return;
			pd.dismiss();
			openSubTree(tree.id);
		}, error -> {
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
			for (FamilyGemTreeInfoModel info : treeInfos) {
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
							infoModel.updatedAt);
					File dirMedia = Helper.getDirMedia(getContext(), nextTreeId);
					tree.dirs.add(dirMedia.getPath());
					tree.isForked = infoModel.isForked;
					Global.settings.addTree(tree);
					Global.settings.save();

					if (getActivity() == null || getActivity().isFinishing())
						return;

					openSubTree(tree.id);
				}, error -> {
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
						}, infoModel -> {
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
									infoModel.updatedAt);
							tree.isForked = true;
							tree.repoStatus = infoModel.repoStatus;
							tree.aheadBy = infoModel.aheadBy;
							tree.behindBy = infoModel.behindBy;
							tree.totalCommits = infoModel.totalCommits;
							Global.settings.addTree(tree);
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
						});
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

	private void unlink() {
		// If multiple link exist
		if (parentFam != null && spouseFam != null) {
			String[] families = { getParentNames(parentFam), getSpuoseAndChildNames(spouseFam) };

			new AlertDialog.Builder(getContext()).setTitle(R.string.which_node)
					.setItems(families, (dialog, which) -> {
						if (which == 0) {
							processUnlink(parentFam);
						} else if (which == 1) {
							processUnlink(spouseFam);
						}
					}).show();
		} else if (parentFam != null) {
			processUnlink(parentFam);
		} else if (spouseFam != null) {
			processUnlink(spouseFam);
		}
	}

	private void processUnlink(Family family) {
		List<Family> modified = new ArrayList<>();
		Famiglia.disconnect(personId, family);
		modified.add(family);
		restore();
		Family[] modificateArr = modified.toArray(new Family[0]);
		U.checkEmptyFamilies(getContext(), this::restore, false, modificateArr);
		U.updateDate(pers);
		U.saveJson(true, (Object[]) modificateArr);
		displaceDiagram();
	}

	private String getParentNames(Family family) {
		List<Person> people = new ArrayList<>();
		people.addAll(family.getHusbands(gc));
		people.addAll(family.getWives(gc));
		return getPersonsNames(people);
	}

	private String getSpuoseAndChildNames(Family family) {
		List<Person> people = new ArrayList<>();
		people.addAll(family.getHusbands(gc));
		people.addAll(family.getWives(gc));
		people.addAll(family.getChildren(gc));
		people.removeIf(p -> p.getId() == personId);
		return getPersonsNames(people);
	}

	private String getPersonsNames(List<Person> people) {
		List<String> names = new ArrayList<>();
		for (Person person : people) {
			names.add(U.getPrincipalName(person));
		}
		String nameJoined = String.join(", ", names);
		return nameJoined;
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
			Log.d("DiagramClick",
					"Current user: " + user.login + ", Repo owner: " + repoOwner + ", Is owner: " + isOwner);
			return isOwner;

		} catch (Exception e) {
			Log.e("DiagramClick", "Error checking tree ownership", e);
			return false;
		}
	}
}
