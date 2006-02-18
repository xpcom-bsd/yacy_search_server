// Bookmarks_p.java 
// -----------------------
// part of YACY
// (C) by Michael Peter Christen; mc@anomic.de
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
//
// This File is contributed by Alexander Schier
// last change: 26.12.2005
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Using this software in any meaning (reading, learning, copying, compiling,
// running) means that you agree that the Author(s) is (are) not responsible
// for cost, loss of data or any harm that may be caused directly or indirectly
// by usage of this softare or this documentation. The usage of this software
// is on your own risk. The installation and usage (starting/running) of this
// software may allow other people or application to access your computer and
// any attached devices and is highly dependent on the configuration of the
// software which must be done by the user of the software; the author(s) is
// (are) also not responsible for proper configuration and usage of the
// software, even if provoked by documentation provided together with
// the software.
//
// Any changes to this file according to the GPL as documented in the file
// gpl.txt aside this file in the shipment you received can be done to the
// lines that follows this copyright notice here, but changes must not be
// done inside the copyright notive above. A re-distribution must contain
// the intact and unchanged copyright notice.
// Contributions and changes to the program code must be marked as such.

// You must compile this file with
// javac -classpath .:../Classes Blacklist_p.java
// if the shell's current path is HTROOT

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Vector;

import de.anomic.data.bookmarksDB;
import de.anomic.data.listManager;
import de.anomic.data.bookmarksDB.Tag;
import de.anomic.http.httpHeader;
import de.anomic.plasma.plasmaSwitchboard;
import de.anomic.plasma.plasmaCrawlLURL;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

public class Bookmarks {
    public static serverObjects respond(httpHeader header, serverObjects post, serverSwitch env) {
    serverObjects prop = new serverObjects();
    plasmaSwitchboard switchboard = (plasmaSwitchboard) env;
    int max_count=10;
    String tagName="";
    int start=0;
    boolean isAdmin=switchboard.verifyAuthentication(header, true);
    
    //defaultvalues
    prop.put("mode", 0);
    if(isAdmin){
        prop.put("mode", 1);
    }
    prop.put("mode_edit", 0);
    prop.put("mode_title", "");
    prop.put("mode_description", "");
    prop.put("mode_url", "");
    prop.put("mode_tags", "");
    prop.put("mode_public", 1); //1=is public
    if(post != null){
        
        if(!isAdmin){
            if(post.containsKey("login")){
                prop.put("AUTHENTICATE","admin log-in");
            }
        }else if(post.containsKey("mode")){
            String mode=(String) post.get("mode");
            if(mode.equals("add")){
            	prop.put("mode", 2);
            }else if(mode.equals("importxml")){
            	prop.put("mode", 3);
            }else if(mode.equals("importbookmarks")){
                prop.put("mode", 4);
            }
        }else if(post.containsKey("add")){ //add an Entry
            String url=(String) post.get("url");
            String title=(String) post.get("title");
            String description=(String) post.get("description");
            String tagsString = (String)post.get("tags");
            if(tagsString.equals("")){
                tagsString="unsorted"; //defaulttag
            }
            Vector tags=listManager.string2vector(tagsString);
        
            bookmarksDB.Bookmark bookmark = switchboard.bookmarksDB.createBookmark(url);
            if(bookmark != null){
                bookmark.setProperty(bookmarksDB.Bookmark.BOOKMARK_TITLE, title);
                bookmark.setProperty(bookmarksDB.Bookmark.BOOKMARK_DESCRIPTION, description);
                if(((String) post.get("public")).equals("public")){
                	bookmark.setPublic(true);
                }else{
                	bookmark.setPublic(false);
                }
                bookmark.setTags(tags, true);
                switchboard.bookmarksDB.setBookmarksTable(bookmark);
                
            }else{
                //ERROR
            }
        }else if(post.containsKey("edit")){
            String urlHash=(String) post.get("edit");
            prop.put("mode", 2);
            if (urlHash.length() == 0) {
                prop.put("mode_edit", 0); // create mode
                prop.put("mode_title", (String) post.get("title"));
                prop.put("mode_description", (String) post.get("description"));
                prop.put("mode_url", (String) post.get("url"));
                prop.put("mode_tags", (String) post.get("tags"));
                prop.put("mode_public", 0);
            } else {
                    bookmarksDB.Bookmark bookmark = switchboard.bookmarksDB.getBookmark(urlHash);
                    if (bookmark == null) {
                        // try to get the bookmark from the LURL database
                        try {
                            plasmaCrawlLURL.Entry urlentry = switchboard.urlPool.loadedURL.getEntry(urlHash, null);
                            prop.put("mode_edit", 0); // create mode
                            prop.put("mode_title", urlentry.descr());
                            prop.put("mode_description", urlentry.descr());
                            prop.put("mode_url", urlentry.url());
                            prop.put("mode_tags", "");
                            prop.put("mode_public", 0);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // get from the bookmark database
                        prop.put("mode_edit", 1); // edit mode
                        prop.put("mode_title", bookmark.getTitle());
                        prop.put("mode_description", bookmark.getDescription());
                        prop.put("mode_url", bookmark.getUrl());
                        prop.put("mode_tags", bookmark.getTags());
                        if (bookmark.getPublic()) {
                            prop.put("mode_public", 1);
                        } else {
                            prop.put("mode_public", 0);
                        }
                    }
                }
        }else if(post.containsKey("bookmarksfile")){
            boolean isPublic=false;
            if(((String) post.get("public")).equals("public")){
                isPublic=true;
            }
            String tags=(String) post.get("tags");
            if(tags.equals("")){
                tags="unsorted";
            }
            try {
                File file=new File((String)post.get("bookmarksfile"));
                switchboard.bookmarksDB.importFromBookmarks(file.toURL() , new String((byte[])post.get("bookmarksfile$file")), tags, isPublic);
            } catch (MalformedURLException e) {}
            
        }else if(post.containsKey("xmlfile")){
        	boolean isPublic=false;
        	if(((String) post.get("public")).equals("public")){
            	isPublic=true;
            }
        	switchboard.bookmarksDB.importFromXML(new String((byte[])post.get("xmlfile$file")), isPublic);
        }else if(post.containsKey("delete")){
            String urlHash=(String) post.get("delete");
            switchboard.bookmarksDB.removeBookmark(urlHash);
        }
        
        if(post.containsKey("tag")){
            tagName=(String) post.get("tag");
        }
        if(post.containsKey("start")){
            start=Integer.parseInt((String) post.get("start"));
        }
        if(post.containsKey("num")){
            max_count=Integer.parseInt((String) post.get("num"));
        }
    }
    Iterator it=switchboard.bookmarksDB.getTagIterator(isAdmin);
    int count=0;
    bookmarksDB.Tag tag;
    prop.put("num-bookmarks", switchboard.bookmarksDB.bookmarksSize());
    while(it.hasNext()){
        tag=(Tag) it.next();
        prop.put("taglist_"+count+"_name", tag.getFriendlyName());
        prop.put("taglist_"+count+"_tag", tag.getTagName());
        prop.put("taglist_"+count+"_num", tag.size());
        count++;
    }
    prop.put("taglist", count);
    count=0;
    if(!tagName.equals("")){
        it=switchboard.bookmarksDB.getBookmarksIterator(tagName, isAdmin);
    }else{
        it=switchboard.bookmarksDB.getBookmarksIterator(isAdmin);
    }
    bookmarksDB.Bookmark bookmark;
    //skip the first entries (display next page)
    count=0;
    while(count < start && it.hasNext()){
        it.next();
        count++;
    }
    count=0;
    Vector tags;
    Iterator tagsIt;
    int tagCount;
    while(count<max_count && it.hasNext()){
        bookmark=switchboard.bookmarksDB.getBookmark((String)it.next());
        if(bookmark!=null){
            prop.put("bookmarks_"+count+"_link", bookmark.getUrl());
            prop.put("bookmarks_"+count+"_title", bookmark.getTitle());
            prop.put("bookmarks_"+count+"_description", bookmark.getDescription());
            prop.put("bookmarks_"+count+"_public", (bookmark.getPublic()? 1:0));
            
            //List Tags.
            tags=bookmark.getTagsVector();
            tagsIt=tags.iterator();
            tagCount=0;
            while(tagsIt.hasNext()){
            	prop.put("bookmarks_"+count+"_tags_"+tagCount+"_tag", tagsIt.next());
            	tagCount++;
            }
            prop.put("bookmarks_"+count+"_tags", tagCount);
            
            prop.put("bookmarks_"+count+"_hash", bookmark.getUrlHash());
            count++;
        }
    }
    prop.put("tag", tagName);
    prop.put("start", start);
    if(it.hasNext()){
        prop.put("next-page", 1);
        prop.put("next-page_start", start+max_count);
        prop.put("next-page_tag", tagName);
        prop.put("next-page_num", max_count);
    }
    if(start >= max_count){
    	start=start-max_count;
    	if(start <0){
    		start=0;
    	}
    	prop.put("prev-page", 1);
    	prop.put("prev-page_start", start);
    	prop.put("prev-page_tag", tagName);
    	prop.put("prev-page_num", max_count);
    }
    prop.put("bookmarks", count);
    return prop;
    }

}
