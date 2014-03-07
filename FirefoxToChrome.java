import java.io.*;
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public class FirefoxToChrome {
	public static void main(String[] args) throws IOException,ParseException {
		if (args.length < 2) {
			System.out.println("USAGE:\njava FirefoxToChrome infile.json outfile.html");
			System.exit(0);
		}
		
		/*
		 * Read and parse file
		 */
		File inputFile = new File(args[0]);
		JSONObject placesRoot = (JSONObject) JSONValue.parseWithException(new FileReader(inputFile));
		ArrayList<Bookmark> bookmarksList = new ArrayList<Bookmark>();
		
		extractPlaces(placesRoot, bookmarksList);
		
		writeHTMLFile(args[1], bookmarksList);
	}
	
	public static void extractPlaces(JSONObject root, ArrayList<Bookmark> bookmarksList) {
		if (root.get("type").equals("text/x-moz-place-container")) {
			if (root.get("root") != null && root.get("root").equals("tagsFolder")) {
				// We have a separate Method for the tags
				extractTags(root, bookmarksList);
			} else {
				// recursion
				JSONArray rootChildren = (JSONArray) root.get("children");
				for (Object child : rootChildren) {
					extractPlaces((JSONObject)child, bookmarksList);
				}
			}
		} else if (root.get("type").equals("text/x-moz-place")) {
			JSONObject place = root;
			
			String title = (String)place.get("title");
			String uri = (String)place.get("uri");
			
			// ignore firefox-specific folders etc.
			if (uri.startsWith("place:")) return;
			
			Bookmark currentBookmark = new Bookmark(title, uri);
			if (! bookmarksList.contains(currentBookmark)) {
				// create a new Bookmark
				bookmarksList.add(currentBookmark);
			} else {
				/*
				 * Some of the entries in tagsFolder have
				 * null-titles.
				 * I don't know why but i'll just fix it here
				 */
				int index = bookmarksList.indexOf(currentBookmark);
				Bookmark oldBookmark = bookmarksList.get(index);
				if (oldBookmark.title == null && currentBookmark.title != null) {
					oldBookmark.title = title;
				}
			}
		}
	}
	
	public static void extractTags(JSONObject tagsFolder, ArrayList<Bookmark> bookmarksList) {
		/*
		 * For each tag...
		 */
		for (Object tagObject : (JSONArray)tagsFolder.get("children")) {
			JSONObject tag = (JSONObject) tagObject;
			JSONArray tagChildren = (JSONArray) tag.get("children");
			String tagTitle = (String)tag.get("title");
			
			/*
			 * ...go through all bookmarks and add the tag to them
			 * if the don't exist, create them first
			 */
			for (Object placeObject : tagChildren) {
				JSONObject place = (JSONObject) placeObject;
				
				String title = (String)place.get("title");
				String uri = (String)place.get("uri");
				Bookmark currentBookmark = new Bookmark(title, uri);
				
				if (! bookmarksList.contains(currentBookmark)) {
					// create a new Bookmark
					bookmarksList.add(currentBookmark);
				}
				
				// add the tag
				int index = bookmarksList.indexOf(currentBookmark);
				Bookmark oldBookmark = bookmarksList.get(index);
				oldBookmark.tags.add(tagTitle);
			}
		}
	}
	
	public static void writeHTMLFile(String filename, ArrayList<Bookmark> bookmarksList) throws IOException {
		PrintWriter pw = new PrintWriter(new FileWriter(new File(filename)));
		
		// header
		pw.println("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n<TITLE>Bookmarks</TITLE>\n<H1>Bookmarks</H1>\n<DL><p>\n\t<DT><H3 ADD_DATE=\"0\" LAST_MODIFIED=\"0\" PERSONAL_TOOLBAR_FOLDER=\"true\">Bookmarks Bar</H3>\n\t<DL><p>\n\t</DL><p>");
		
		// bookmarks
		for (Bookmark b : bookmarksList) {
			pw.println("\t<DT><A HREF=\"" + b.uri + "\" ADD_DATE=\"0\" LAST_VISIT=\"0\" LAST_MODIFIED=\"0\">" + b.toString() + "</A>");
		}
		
		// footer
		pw.println("</DL><p>");
		
		pw.flush();
		pw.close();
	}
}