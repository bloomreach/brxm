/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.tools.dropbox;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v1Tag;

public class MP3Box extends Dropbox {

    /**
     * Contructor
     * @param repoLoc
     * @param dropbox
     * @throws RepositoryException
     */
    public MP3Box(String repoLoc, String dropbox) throws RepositoryException {
        super(repoLoc, dropbox);
    }

    /**
     * Run me....
     * @param args
     */
    public static void main(String[] args) {
        if (args == null || args.length != 4) {
            usage();
        } else {
            try {
                MP3Box box = new MP3Box(args[0], args[1]);
                box.setCredentials(new SimpleCredentials(args[2], args[3].toCharArray()));
                box.drop("mp3box");
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initialize example facets
     */
    protected void initFacets(Session session) throws RepositoryException {
        String docbase = "/mp3box";
        String navRootName = "mp3nav";

        // create/reset the navigation base
        System.out.println("Initializing facets.");
        Node root = session.getRootNode();
        if (root.hasNode(navRootName)) {
            root.getNode(navRootName).remove();
        }
        Node navRoot = root.addNode(navRootName);

        // some demo facets
        createFacet(navRoot, docbase, "byGenreYearArtist", new String[] { "genre", "year", "artist" });
        createFacet(navRoot, docbase, "byYearGenreArtist", new String[] { "year", "genre", "artist" });
        createFacet(navRoot, docbase, "byArtistAlbum", new String[] { "artist", "album" });
        createFacet(navRoot, docbase, "byArtistTitle", new String[] { "artist", "title" });
        createFacet(navRoot, docbase, "byArtistYear", new String[] { "artist", "year" });
        createFacet(navRoot, docbase, "byYearAlbum", new String[] { "year", "album" });
        createFacet(navRoot, docbase, "byYearTitle", new String[] { "year", "title" });
        createFacet(navRoot, docbase, "byYearArtist", new String[] { "year", "artist" });
        createFacet(navRoot, docbase, "byGenreArtist", new String[] { "genre", "artist" });
        createFacet(navRoot, docbase, "byAlbum", new String[] { "album" });
        createFacet(navRoot, docbase, "byTitle", new String[] { "title" });
        createFacet(navRoot, docbase, "byYear", new String[] { "year" });
        createFacet(navRoot, docbase, "byGenre", new String[] { "genre" });
    }

    /**
     * Extract the mp3 tag info and store meta info in jcr
     */
    protected Node createFile(Node folder, File f) throws RepositoryException {
        Node n = super.createFile(folder, f, true);
        Property prop = n.getProperty("mimeType");
        String mimeType = prop.getString();

        // be quiet please
        Logger logger = Logger.getLogger("org.jaudiotagger");
        logger.setLevel(Level.WARNING);

        // parse audio files
        if ("audio/mpeg".equals(mimeType)) {
            try {
                MP3File mp3 = (MP3File) AudioFileIO.read(f);
                MP3AudioHeader audioHeader = mp3.getMP3AudioHeader();
                Tag tag = mp3.getTag();

                Song song = new Song(audioHeader, tag);
                song.persistToNode(n);
                //System.out.println(song);

            } catch (CannotReadException e) {
                System.out.println("CannotReadException: Unable to parse : " + e.getMessage());
            } catch (TagException e) {
                System.out.println("TagException: Unable to parse : " + e.getMessage());
            } catch (ReadOnlyFileException e) {
                System.out.println("ReadOnlyFileException: Unable to parse : " + e.getMessage());
            } catch (InvalidAudioFrameException e) {
                System.out.println("InvalidAudioFrameException: Unable to parse : " + e.getMessage());
            } catch (IOException e) {
                System.out.println("IOException: Unable to parse : " + e.getMessage());
            }
        }
        return n;
    }

    /**
     * Song wrapper
     */
    private class Song {
        // defaults
        String title = "unknown";
        String album = "unknown";
        String artist = "unknown";
        String genre = "unknown";
        String year = "unknown";
        String track;
        String channels;
        String encoding;
        int length = -1;
        int sampleRate = -1;
        long bitRate = -1;
        boolean isVariableBitRate = false;
        boolean isV1 = false;

        /**
         * Create song wrapper
         * @param ah the AudioHeader
         * @param tag the Audion Tag
         */
        public Song(AudioHeader ah, Tag tag) {
            if (tag instanceof ID3v1Tag) {
                isV1 = true;
            }
            setAlbum(tag.getFirstAlbum());
            setArtist(tag.getFirstArtist());
            setBitRate(ah.getBitRateAsNumber());
            setChannels(ah.getChannels());
            setEncoding(ah.getEncodingType());
            setGenre(tag.getFirstGenre());
            setLength(ah.getTrackLength());
            setSampleRate(ah.getSampleRateAsNumber());
            setTitle(tag.getFirstTitle());
            setYear(tag.getFirstYear());
            setVariableBitRate(ah.isVariableBitRate());
            if (!isV1) {
                setTrack(tag.getFirstTrack());
            }
        }

        /**
         * Persist the song in jcr
         * @param n the node
         * @throws RepositoryException
         */
        public void persistToNode(Node n) throws RepositoryException {
            n.setProperty("album", getAlbum());
            n.setProperty("artist", getArtist());
            n.setProperty("genre", getGenre());
            n.setProperty("title", getTitle());
            n.setProperty("year", getYear());
            if (getLength() > 0)
                n.setProperty("trackLength", getLength());
            if (getSampleRate() > 0)
                n.setProperty("sampleRate", getSampleRate());
            if (getBitRate() > 0)
                n.setProperty("bitRate", getBitRate());
            if (getChannels() != null)
                n.setProperty("channels", getChannels());
            n.setProperty("encoding", getEncoding());
            n.setProperty("variableBitRate", isVariableBitRate());
            if (!isV1) {
                n.setProperty("track", getTrack());
            }
        }

        public boolean isSet(String s) {
            if (s == null || s.length() == 0) {
                return false;
            }
            return true;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            if (isSet(album))
                this.album = album;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            if (isSet(artist))
                this.artist = artist;
        }

        public long getBitRate() {
            return bitRate;
        }

        public void setBitRate(long bitrate) {
            this.bitRate = bitrate;
        }

        public String getChannels() {
            return channels;
        }

        public void setChannels(String channels) {
            if (isSet(channels))
                this.channels = channels;
        }

        public String getEncoding() {
            return encoding;
        }

        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            if (isSet(genre))
                this.genre = genre;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int samplerate) {
            this.sampleRate = samplerate;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            if (isSet(title))
                this.title = title;
        }

        public String getTrack() {
            return track;
        }

        public void setTrack(String track) {
            this.track = track;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            if (isSet(year))
                this.year = year;
        }

        public String toString() {
            return artist + " > " + album + " > " + title + " > " + channels + " > " + bitRate;

        }

        public boolean isVariableBitRate() {
            return isVariableBitRate;
        }

        public void setVariableBitRate(boolean isVariableBitRate) {
            this.isVariableBitRate = isVariableBitRate;
        }

    }

}
