package app.familygem;

import org.folg.gedcom.model.Gedcom;

/**
 * Custom Diagram fragment for kinship path visualization that restores the original gedcom
 * when the fragment is destroyed or detached.
 */
public class KinshipDiagram extends Diagram {
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        KinshipPathExtractor.restoreOriginalGedcom();
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        KinshipPathExtractor.restoreOriginalGedcom();
    }
}