package org.ops4j.pax.web.resources.jsf.internal;

import java.util.Comparator;

/**
 * FIXME DISCLAIMER MyFaces
 */
public final class VersionComparator implements Comparator<String>
{

    public int compare(String s1, String s2)
    {
        int n1 = 0;
        int n2 = 0;
        String o1 = s1;
        String o2 = s2;

        boolean p1 = true;
        boolean p2 = true;

        while (n1 == n2 && (p1 || p2))
        {
            int i1 = o1.indexOf('_');
            int i2 = o2.indexOf('_');
            if (i1 < 0)
            {
                if (o1.length() > 0)
                {
                    p1 = false;
                    n1 = Integer.valueOf(o1);
                    o1 = "";
                }
                else
                {
                    p1 = false;
                    n1 = 0;
                }
            }
            else
            {
                n1 = Integer.valueOf(o1.substring(0, i1));
                o1 = o1.substring(i1 + 1);
            }
            if (i2 < 0)
            {
                if (o2.length() > 0)
                {
                    p2 = false;
                    n2 = Integer.valueOf(o2);
                    o2 = "";
                }
                else
                {
                    p2 = false;
                    n2 = 0;
                }
            }
            else
            {
                n2 = Integer.valueOf(o2.substring(0, i2));
                o2 = o2.substring(i2 + 1);
            }
        }

        if (n1 == n2)
        {
            return s1.length() - s2.length();
        }
        return n1 - n2;
    }
}
