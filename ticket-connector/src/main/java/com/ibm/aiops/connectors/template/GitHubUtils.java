package com.ibm.aiops.connectors.template;

public class GitHubUtils {
    // Give the number of comments, get back the query with pages and limits
    // per_page and page
    public static String getLastCommentPage(int comments, int perPage) {
        int pageNum = Math.floorDiv(comments, perPage);
        int remainder = Math.floorMod(comments, perPage);

        // An extra page is needed
        if (remainder > 0) {
            pageNum++;
        }
        return "per_page=" + perPage + "&page=" + pageNum;
    }
}
