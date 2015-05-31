package net.melissam.powerlog.normalisation;

/**
 * Type of normalisation process that can be chosen.
 * 
 * @author melissam
 *
 */
public enum NormalisationProcess {

	// no normalisation of data set
	NONE,	
	
	// normalise the whole data set before processing
	BEFORE,
	
	// periodic processing on sliding windows 
	INSTREAM;
	
	public static NormalisationProcess fromName(String name){
		
		NormalisationProcess np = null;
		
		for (NormalisationProcess _np : NormalisationProcess.values()){
			if (_np.name().equalsIgnoreCase(name)){
				np = _np;
			}
		}
		
		return np != null ? np : NormalisationProcess.NONE;
	}
	
}
