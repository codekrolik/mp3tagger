package net.mp3tagger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.pillar.log4j.Log4JUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Mp3Tagger {
	static final Logger logger = Logger.getLogger(Mp3Tagger.class);

	public static void main(String[] argv) {
		Log4JUtils.debugInitLog4j();

		JCommander cmd = null;
		Mp3TaggerArgs args = new Mp3TaggerArgs();
		try {
			cmd =  JCommander.newBuilder()
					.addObject(args)
					.build();

			cmd.parse(argv);
		} catch (ParameterException e) {
			logger.error(e, e);

			e.usage();
			System.exit(1);
		}

		if (args.folder == null) {
			cmd.usage();
			System.exit(1);
		}

		File folder = new File(args.folder);

		if (!folder.isDirectory()) {
			logger.info("Folder should exist and be a directory, not a file");
			cmd.usage();
			System.exit(1);
		}

		Mp3Tagger mp3Tagger = new Mp3Tagger();

		logger.info(
				String.format("STARTING: main directory [%s]", folder.getPath())
			);
		mp3Tagger.setDirectoryTags(folder);
		logger.info(
				String.format("FINISHED: main directory [%s]", folder.getPath())
			);
	}

	double getTrack(File f) {
		String filename = f.getName();
		try {
			String[] fileParts1 = filename.substring(0, filename.length() - 4).trim().split("-");
			String[] fileParts2 = fileParts1[0].trim().split(" ");

			String track = fileParts2[0].trim();
			return Double.parseDouble(track);
		} catch (NumberFormatException nfe) {
			logger.error(
					String.format("Error parsing track number from file [%s], defaulting to Double.MAX_VALUE", filename)
				);
			return Double.MAX_VALUE;
		}
	}

	void setDirectoryTags(File f) {
		logger.info(
				String.format("Processing directory [%s]", f.getPath())
			);
		File[] files = f.listFiles();
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return Double.compare(getTrack(o1), getTrack(o2));
			}
		});

		int fileIndex = 0;
		for (File file : files) {
			try {
				if (file.isDirectory()) {
					setDirectoryTags(file);
				} else {
					setFileTags(file, ++fileIndex);
				}
			} catch (Exception e) {
				logger.error(
						String.format("Error processing file [%s]", file.getPath()),
						e
					);
			}
		}
	}

	/**
	 * Format: DIRECTORY/FILE.mp3
	 * DIRECTORY:
	 * 	[artist] [year]-[album_name]
	 * FILE:
	 * 	[track_num] [artist] - [track_name].mp3
	 *
	 * @param f
	 * @throws UnsupportedTagException
	 * @throws InvalidDataException
	 * @throws IOException
	 * @throws TagException
	 * @throws InvalidAudioFrameException
	 * @throws ReadOnlyFileException
	 * @throws CannotReadException
	 * @throws CannotWriteException
	 */
	void setFileTags(File f, int fileIndex) throws IOException, TagException, CannotReadException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
		logger.info(
				String.format("Processing file [%s]", f.getPath())
			);

		File dir = f.getParentFile();

		String[] dirParts1 = dir.getName().trim().split("-");

		String dirPart1 = dirParts1[0];
		String album = dir.getName().substring(dirPart1.length() + 1).trim();

		dirPart1 = dirPart1.trim();
		String[] dirParts2 = dirPart1.split(" ");

		String year = dirParts2[dirParts2.length - 1];
		String albumArtist = dirPart1.substring(0, dirPart1.length() - year.length() - 1);
		year = year.trim();

		String filename = f.getName();
		String[] fileParts1 = filename.substring(0, filename.length() - 4).trim().split("-");
		String filePart1 = fileParts1[0].trim();

		String artist = filePart1.substring(filePart1.indexOf(' ')).trim();
		String title = fileParts1[fileParts1.length - 1].trim();

		//Open MP3File
		MP3File mp3File = (MP3File)AudioFileIO.read(f);

		Tag tag = mp3File.getTag();
		if (tag == null) {
			tag = mp3File.createDefaultTag();
			mp3File.setTag(tag);
		}

		tag.setField(FieldKey.ARTIST, artist);
		tag.setField(FieldKey.YEAR, year);
		tag.setField(FieldKey.ALBUM, album);
		tag.setField(FieldKey.TITLE, title);
		tag.setField(FieldKey.ALBUM_ARTIST, albumArtist);
		tag.setField(FieldKey.TRACK, Integer.toString(fileIndex));

		AudioFileIO.write(mp3File);
	}
}
