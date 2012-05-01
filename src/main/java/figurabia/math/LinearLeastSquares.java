/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 15.06.2009
 */
package figurabia.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class LinearLeastSquares {

    public static List<Long> fitToBars(List<Long> ones, List<Long> fives) {
        Collections.sort(ones);
        Collections.sort(fives);
        
        SortedMap<Long, Integer> beats = new TreeMap<Long, Integer>();
        for( Long time : ones)
            beats.put(time, 1);
        for( Long time : fives)
            beats.put(time, 5);
        
        // match to coordinates (better simple than complicated)
        int lastType = 5;
        int virtualIdealTime = -5;
        long lastMeasuredTime = -1;
        for( Long measuredTime : new TreeSet<Long>(beats.keySet())) {
            int currentType = beats.get(measuredTime); // get beat type (1 or 5) of current beat

            
            // add a beat inbetween if the alternating beat is missing
            if( currentType == lastType && lastMeasuredTime != -1) {
                virtualIdealTime += 5;
                long timeInbetween = (measuredTime + lastMeasuredTime) / 2;
                beats.put(timeInbetween, virtualIdealTime);
            }
            
            virtualIdealTime += 5;
            beats.put(measuredTime, virtualIdealTime);
            
            // update last values for next iteration
            lastMeasuredTime = measuredTime;
            lastType = currentType;
        }
        
        // do the linear least squares algorithm
        return linearLeastSquareFitted(beats);
    }
    
    public static List<Long> linearLeastSquareFitted(SortedMap<Long,Integer> coordinates) {
        // find parameters of line equation (using least squares) (x goes from 0 to size-1)
        Set<Long> keySet = coordinates.keySet();
        int n = keySet.size();
        double sumX = 0;
        double sumXSquare = 0;
        for( int x = 0; x < n; x++) {
            sumX += x;
            sumXSquare += x*x;
        }
        double sumY = 0;
        double sumXY = 0;
        int x = 0;
        for( long y : keySet) {
            sumY += y;
            sumXY += y*x;
            x++;
        }
        // parameters according to the linear equation: y = a*x + b
        double b = (sumXSquare * sumY - sumXY * sumX) / (n * sumXSquare - sumX * sumX);
        double a = (n * sumXY - sumX * sumY) / (n * sumXSquare - sumX * sumX);
        
        // recreate time points from equation
        List<Long> newValues = new ArrayList<Long>(n);
        for( x = 0; x < n; x++) {
            newValues.add((long)(a*(double)x + b));
        }
        return newValues;
    }
}
