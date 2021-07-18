package org.xersys.benta.dto;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.json.simple.JSONObject;
import org.xersys.kumander.iface.XEntity;

@Entity
@Table(name="Sales_Detail")

public class Sales_Detail implements Serializable, XEntity {
    @Id
    @Basic(optional = false)
    @Column(name = "sTransNox")
    private String sTransNox;
        
    @Column(name = "nEntryNox")
    private int nEntryNox;
    
    @Column(name = "sOrderNox")
    private String sOrderNox;
    
    @Column(name = "sStockIDx")
    private String sStockIDx;
    
    @Column(name = "nQuantity")
    private int nQuantity;
    
    @Column(name = "nInvCostx")
    private Number nInvCostx;
    
    @Column(name = "nUnitPrce")
    private Number nUnitPrce;
    
    @Column(name = "nDiscount")
    private Number nDiscount;
    
    @Column(name = "nAddDiscx")
    private Number nAddDiscx;
    
    @Column(name = "sSerialID")
    private String sSerialID;
    
    @Column(name = "cNewStock")
    private String cNewStock;
    
    @Column(name = "sNotesxxx")
    private String sNotesxxx;
    
    LinkedList laColumns = null;
    
    public Sales_Detail(){
        laColumns = new LinkedList();
        
        laColumns.add("sTransNox");
        laColumns.add("nEntryNox");
        laColumns.add("sOrderNox");
        laColumns.add("sStockIDx");
        laColumns.add("nQuantity");
        laColumns.add("nInvCostx");
        laColumns.add("nUnitPrce");
        laColumns.add("nDiscount");
        laColumns.add("nAddDiscx");
        laColumns.add("sSerialID");
        laColumns.add("cNewStock");
        laColumns.add("sNotesxxx");
        
        sTransNox = "";
        nEntryNox = -1;
        sOrderNox = "";
        sStockIDx = "";
        nQuantity = 0;
        nInvCostx = 0.00;
        nUnitPrce = 0.00;
        nDiscount = 0.00;
        nAddDiscx = 0.00;
        sSerialID = "";
        cNewStock = "1";
        sNotesxxx = "";
    }
    
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Sales_Detail)) return false;
        
        Sales_Detail other = (Sales_Detail) object;
        
        return !((sTransNox == null && other.sTransNox != null) || 
                (sTransNox != null && !sTransNox.equals(other.sTransNox))) &&
                nEntryNox != other.nEntryNox;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(sTransNox);
        hash = 47 * hash + nEntryNox;
        return hash;
    }
    
    @Override
    public String toString() {
        return this.getClass().getName() + "[sTransNox=" + sTransNox + ", nEntryNox=" + nEntryNox + "]";
    }
    
    @Override
    public Object getValue(int fnColumn) {
        switch(fnColumn){
            case 1: return sTransNox;
            case 2: return nEntryNox;
            case 3: return sOrderNox;
            case 4: return sStockIDx;
            case 5: return nQuantity;
            case 6: return nInvCostx;
            case 7: return nUnitPrce;
            case 8: return nDiscount;
            case 9: return nAddDiscx;
            case 10: return sSerialID;
            case 11: return cNewStock;
            case 12: return sNotesxxx;
            default: return null;
        }
    }

    @Override
    public Object getValue(String fsColumn) {
        int lnCol = getColumn(fsColumn);
        
        if (lnCol > 0){
            return getValue(lnCol);
        } else
            return null;
    }

    @Override
    public String getColumn(int fnCol) {
        if (laColumns.size() < fnCol){
            return "";
        } else 
            return (String) laColumns.get(fnCol - 1);
    }

    @Override
    public int getColumn(String fsCol) {
        return laColumns.indexOf(fsCol) + 1;
    }

    @Override
    public void setValue(int fnColumn, Object foValue) {
        switch(fnColumn){
            case 1: sTransNox = (String) foValue; break;
            case 2: nEntryNox = Integer.parseInt(String.valueOf(foValue)); break;
            case 3: sOrderNox = (String) foValue; break;
            case 4: sStockIDx = (String) foValue; break;
            case 5: nQuantity = Integer.parseInt(String.valueOf(foValue)); break;
            case 6: nInvCostx = (Number) foValue; break;
            case 7: nUnitPrce = (Number) foValue; break;
            case 8: nDiscount = (Number) foValue; break;
            case 9: nAddDiscx = (Number) foValue; break;
            case 10: sSerialID = (String) foValue; break;
            case 11: cNewStock = (String) foValue; break;
            case 12: sNotesxxx = (String) foValue; break;
        }     
    }

    @Override
    public void setValue(String fsColumn, Object foValue) {
        int lnCol = getColumn(fsColumn);
        if (lnCol > 0){
            setValue(lnCol, foValue);
        }
    }

    @Override
    public int getColumnCount() {
        return laColumns.size();
    }

    @Override
    public String getTable() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toJSONString() {
        JSONObject loJSON = new JSONObject();
        
        for(int i = 0; i < laColumns.size(); i++){
            loJSON.put(laColumns.get(i), getValue(getColumn(i + 1)));
        }
        
        return loJSON.toJSONString();
    }

    @Override
    public void list() {
        for(int i = 0; i < laColumns.size(); i++){
            System.out.println(laColumns.get(i));
        }
    }
}
