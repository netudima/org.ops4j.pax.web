package org.ops4j.pax.web.resources.api;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO
 */
public class ResourceQuery {

    private String startSegment;
    private Set<String> segments = new HashSet<>(5);
    private String endSegment;

    public ResourceQuery startsWith(String segment){
        if(this.startSegment != null){
            throw new IllegalStateException("startsWith was already set previously");
        }
        if(isNullOrEmpty(segment)) {
            startSegment = segment;
        }
        return this;
    }

    /**
     * Specify an path-segment which is allowed in any location
     * @param segment the segment to
     * @return
     */
    public ResourceQuery withSegment(String segment){
        // avoid emtpy strings
        if(isNullOrEmpty(segment)) {
            this.segments.add(segment);
        }
        return this;
    }

    public ResourceQuery endsWith(String segment){
        if(this.endSegment != null){
            throw new IllegalStateException("endsWith was already set previously");
        }
        if(isNullOrEmpty(segment)) {
            endSegment = segment;
        }
        return this;
    }

    private boolean isNullOrEmpty(String s){
        return s != null && s.trim().length() > 0;
    }

    public boolean matches(String path){
        boolean matchesStart = true;
        boolean matchesSegments = true;
        boolean matchesEnd = true;

        if(startSegment != null){
            matchesStart = path.startsWith(startSegment);
        }

        for(String segment : segments){
            if(!path.contains(segment)){
                matchesSegments = false;
                break;
            }
        }

        if(endSegment != null){
            matchesEnd = path.endsWith(endSegment);
        }

        return matchesStart && matchesSegments && matchesEnd;
    }
}
