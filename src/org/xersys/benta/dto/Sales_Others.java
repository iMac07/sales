package org.xersys.benta.dto;

import java.util.LinkedList;
import org.json.simple.JSONObject;

public class Sales_Others{
    String sBarCodex;
    String sDescript;
    String sOtherInf;
    Number nUnitPrce;
    int nQtyOnHnd;
    
    LinkedList laColumns = null;
    
    public Sales_Others(){
        laColumns = new LinkedList();
        laColumns.add("sBarCodex");
        laColumns.add("sDescript");
        laColumns.add("sOtherInf");
        laColumns.add("nUnitPrce");
        laColumns.add("nQtyOnHnd");
        
        sBarCodex = "";
        sDescript = "";
        sOtherInf = "";
        nUnitPrce = 0.00;
        nQtyOnHnd = 0;
    }
    
    public Object getValue(int fnColumn) {
        switch(fnColumn){
            case 1: return sBarCodex;
            case 2: return sDescript;
            case 3: return sOtherInf;
            case 4: return nUnitPrce;
            case 5: return nQtyOnHnd;
            default: return null;
        }
    }

    public Object getValue(String fsColumn) {
        int lnCol = getColumn(fsColumn);
        
        if (lnCol > 0){
            return getValue(lnCol);
        } else
            return null;
    }

    public String getColumn(int fnCol) {
        if (laColumns.size() < fnCol){
            return "";
        } else 
            return (String) laColumns.get(fnCol - 1);
    }

    public int getColumn(String fsCol) {
        return laColumns.indexOf(fsCol) + 1;
    }

    public void setValue(int fnColumn, Object foValue) {
        switch(fnColumn){
            case 1: sBarCodex = (String) foValue; break;
            case 2: sDescript = (String) foValue; break;
            case 3: sOtherInf = (String) foValue; break;
            case 4: nUnitPrce = (Number) foValue; break;
            case 5: nQtyOnHnd = (int) (long) foValue; break;
        }     
    }

    public void setValue(String fsColumn, Object foValue) {
        int lnCol = getColumn(fsColumn);
        if (lnCol > 0){
            setValue(lnCol, foValue);
        }
    }
    
    public String toJSONString() {
        JSONObject loJSON = new JSONObject();
        
        for(int i = 0; i < laColumns.size(); i++){
            loJSON.put(laColumns.get(i), getValue(getColumn(i + 1)));
        }
        
        return loJSON.toJSONString();
    }
    
    public void setBarCode(String fsValue){
        sBarCodex = fsValue;
    }
    public String getBarCode(){
        return sBarCodex;
    }
    
    public void setDescript(String fsValue){
        sDescript = fsValue;
    }
    public String getDescript(){
        return sDescript;
    }
    
    public void setOtherInfo(String fsValue){
        sOtherInf = fsValue;
    }
    public String getOtherInfo(){
        return sOtherInf;
    }
    
    public void setUnitPrice(Number fnValue){
        nUnitPrce = fnValue;
    }
    public Number getUnitPrice(){
        return nUnitPrce;
    }
    
    public void setQtyOnHand(int fnValue){
        nQtyOnHnd = fnValue;
    }
    public int getQtyOnHand(){
        return nQtyOnHnd;
    }
}
