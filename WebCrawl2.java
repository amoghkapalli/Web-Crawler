import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Amogh Kapalli
 * WebCrawl.java
 * This program takes two arguments from the command line: a url and a number of hops to be performed
 * The program performs those hops parsing through html files to the next unique <a href> tag.
 * NOTE: The program looks for the next "<a href>" tag exactly in that form in the html files.
 */
public class WebCrawl2 {

    public static void main(String[] args){

        if(args.length != 2){
            System.err.println("Two arguments were not inputted");
            return;
        }
        //parse cli inputs into url string and number of hops(int)
        int hopsRequested;
        try{
            hopsRequested = Integer.parseInt(args[1]);
        }
        catch(NumberFormatException nfe){
            throw new NumberFormatException("Integer not inputted");
        }

        String stringUrl = args[0];
        //String stringUrl="https://courses.washington.edu/css502/dimpsey/";

        stringUrl = shortenURL(stringUrl);
        //System.out.println(stringUrl);
        LinkedList<String> orderedLinks = new LinkedList<String>();
        Set<String> uniqueLinks = new HashSet<String>();
        orderedLinks.add(stringUrl);
        execute(hopsRequested, orderedLinks, uniqueLinks);


    }

    /**
     *
     * @param stringUrl: URL of a string
     * @return Shortened string without a trailing backslash and shortens links to start with http
     */
    public static String shortenURL(String stringUrl){

        int end = stringUrl.length()-1;
        if(stringUrl.charAt(end) == '/'){
            stringUrl = stringUrl.substring(0, end);
        }
        return stringUrl;
    }

    /**
     *
     * @param stringUrl: URL for a string
     * @return true if URL is not malformed
     */
    public static boolean checkValidURL(String stringUrl){
        try {
            URL validURL = new URL(stringUrl);
            return true;
        }
        catch(MalformedURLException e){
            System.err.println("Incorrect URL inputted");
            return false;
        }
    }

    /**
     *
     * @param hops: Number of hops to be executed
     * @param orderedLinks: LinkedList for the order of links for backtracking
     * @param uniqueLinks: HashSet for the unique list of visited links
     */
    public static void execute(int hops, LinkedList<String> orderedLinks, Set<String> uniqueLinks) {

        int hopsRequested=hops+1;
        boolean redirect=false;
        while(hopsRequested >0){
            if(orderedLinks.isEmpty()){
                System.out.println("no more links remaining");
                return;
            }
            else{
                String strurl = orderedLinks.peek();
                strurl = shortenURL(strurl);
                if(checkValidURL(strurl)){
                    uniqueLinks.add(shortenURL(strurl));
                    try {
                        URL urlLink = new URL(strurl);
                        //connection to fetch resources
                        HttpURLConnection connection = (HttpURLConnection)urlLink.openConnection();
                        int response = connection.getResponseCode();
                        //accepted connection
                        if(response >=200 && response<=299){
                            hopsRequested--;
                        }
                        //redirect connection
                        else if(response >= 300 && response <= 399){
                            //redirect link
                            String temp=connection.getHeaderField("Location");
                            temp=shortenURL(temp);
                            //if redirect link is in unique links, backtrack to the previous link.
                            if(uniqueLinks.contains(temp)){
                                System.out.println("The redirect link has already been visited");
                                orderedLinks.remove();
                                strurl=orderedLinks.peek();
                            }
                            //otherwise continue to the new redirected link
                            else{
                                System.out.println(strurl+ " redirected to " + temp);
                                strurl=temp;
                                hopsRequested--;
                                redirect = true;
                            }
                            //open connection to the new url
                            if(checkValidURL(strurl)){
                                urlLink = new URL(strurl);
                                connection = (HttpURLConnection)urlLink.openConnection();
                            }
                            else{ return; }
                        }
                        //Client error codes
                        else if(response >= 400 && response <= 499){
                            //backtrack to previous url
                            orderedLinks.remove();
                            strurl=orderedLinks.peek();
                            urlLink = new URL(strurl);
                            connection = (HttpURLConnection)urlLink.openConnection();
                            throw new ConnectException("There is a issue on the clients side");
                        }
                        //Server error codes
                        else if(response >= 500 && response <= 599){
                            //backtrack to previous url
                            orderedLinks.remove();
                            strurl=orderedLinks.peek();
                            urlLink = new URL(strurl);
                            connection = (HttpURLConnection)urlLink.openConnection();
                            throw new UnknownHostException();
                        }
                        if(redirect==true){
                            redirect=false;
                            uniqueLinks.add(strurl);
                        }

                        System.out.println("Link number: " + (hops-hopsRequested) + " url: " + strurl);

                        //Parsing the html files to extract the next <a href> reference
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        boolean linkFlag=false;
                        Pattern pattern = Pattern.compile("<a href=\"(http.*?)\"/?>?");
                        Matcher matcher;
                        String currLine;
                        while ((currLine = reader.readLine()) != null) {
                            if(linkFlag){
                                break;
                            }
                            if(hopsRequested==0){
                                break;
                            }
                            matcher = pattern.matcher(currLine);
                            while(matcher.find()){
                                if(!uniqueLinks.contains(shortenURL(matcher.group(1)))) {
                                    linkFlag = true;
                                    orderedLinks.addFirst(shortenURL(matcher.group(1)));
                                    break;
                                }
                            }
                        }
                        //if no <a href> reference was retrieved from the file, and there are still hops to be completed, then break
                        if(!linkFlag && hopsRequested!=0){
                            orderedLinks.remove(strurl);
                            System.out.println("No more <a href  >  links at: " + strurl);
                            break;
                        }

                    }
                    catch(IOException i){
                        System.out.println("ERROR: " + i.getMessage());
                    }

                }
            }

        }
    }


}
