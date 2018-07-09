package net.mp3tagger;

import com.beust.jcommander.Parameter;

public class Mp3TaggerArgs {
	@Parameter(names = {"-f", "--folder"} )
	public String folder;
}
