package com.bfv.model;

import com.bfv.DataSource;
import com.bfv.VarioChangeListener;
import com.bfv.util.RegressionSlope;

import java.util.ArrayList;

public class Vario implements DataSource {

    public static int VAR_USE_DAMP_ALT = 1;
    public static int VAR_USE_RAW_ALT = 2;


    private double damp;
    private int windowSize;
    private boolean windowFull;
    private String name;


    private int r;

    private double var;
    private Altitude altitude;

    private int varUseAltType;

    private double lastVar;

    private RegressionSlope regression;

    private double[][] window;

    private ArrayList<VarioChangeListener> varioChangeListeners;

    private double maxVarSinceLast = -1000.0;
    private double minVarSinceLast = 1000.0;
    private double avgVarSinceLast = 0.0;
    private int countSinceLast = 0;

    public Vario(double damp, int windowSize, String name, int varUseAltType) {
        this.damp = damp;
        this.varUseAltType = varUseAltType;
        regression = new RegressionSlope();
        this.setWindowSize(windowSize);
        this.name = name;

    }


    public void setDamp(double damp) {
        this.damp = damp;
    }

    public void setVarUseAltType(int varUseAltType) {
        this.varUseAltType = varUseAltType;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
        window = new double[windowSize][2];
        r = 0;
        regression.clear();
        windowFull = false;
        var = 0.0;
    }

    public void resetWindow() {
        this.setWindowSize(windowSize);
    }

    public synchronized double addData(double time) {

        if (windowFull) {
            regression.removeData(window[r][0], window[r][1]);
        }

        window[r][0] = time;
        if (varUseAltType == Vario.VAR_USE_DAMP_ALT) {
            window[r][1] = altitude.getDampedAltitude();
        } else if (varUseAltType == Vario.VAR_USE_RAW_ALT) {
            window[r][1] = altitude.getRawAltitude();
        } else {                                       //default
            window[r][1] = altitude.getRawAltitude();
        }


        regression.addData(window[r][0], window[r][1]);


        r++;
        if (r == windowSize) {
            r = 0;
            windowFull = true;
        }
        //rawVar
        double rawVar = regression.getSlope();

        if (!Double.isNaN(rawVar) && windowFull) {

            var = var + damp * (rawVar - var);

        }

        //if(Math.abs(lastVar-var) > 0.1){  //todo change notice
        this.notifyListeners(var);
        //}

        lastVar = var;


        if (var > maxVarSinceLast) {
            maxVarSinceLast = var;
        }
        if (var < minVarSinceLast) {
            minVarSinceLast = var;
        }
        countSinceLast++;
        avgVarSinceLast += (var - avgVarSinceLast) / countSinceLast;
        return var;


    }

    private void notifyListeners(double changedVar) {
        if (varioChangeListeners == null) {
            return;
        }
        for (int i = 0; i < varioChangeListeners.size(); i++) {
            VarioChangeListener varioChangeListener = varioChangeListeners.get(i);

            varioChangeListener.varioChanged(changedVar);

        }

    }


    public synchronized double getValue() {
        return var;
    }

    public void registerAltitude(Altitude alt) {
        this.altitude = alt;
    }

    public String getName() {
        return name;
    }

    public synchronized void addChangeListener(VarioChangeListener varioChangeListener) {
        if (varioChangeListeners == null) {
            varioChangeListeners = new ArrayList<VarioChangeListener>();

        }
        varioChangeListeners.add(varioChangeListener);
    }

    public synchronized void removeChangeListener(VarioChangeListener varioChangeListener) {
        if (varioChangeListeners != null && varioChangeListeners.contains(varioChangeListener)) {
            varioChangeListeners.remove(varioChangeListener);

        }
    }

    public synchronized double[] getMinMaxAvgSinceLast() {
        double[] ret = null;
        if (countSinceLast > 0) {
            ret = new double[]{minVarSinceLast, maxVarSinceLast, avgVarSinceLast, countSinceLast};
        } else {
            ret = new double[]{0.0, 0.0, 0.0, 0.0};
        }
        maxVarSinceLast = -1000.0;
        minVarSinceLast = 1000.0;
        avgVarSinceLast = 0.0;
        countSinceLast = 0;
        return ret;
    }


}