package org.xersys.benta.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.json.simple.JSONObject;
import org.xersys.kumander.iface.XEntity;
import org.xersys.kumander.util.SQLUtil;

@Entity
@Table(name="Sales_Master")

public class Sales_Master implements Serializable, XEntity {
    @Id
    @Basic(optional = false)
    @Column(name = "sTransNox")
    private String sTransNox;
    
    @Column(name = "sBranchCd")
    private String sBranchCd;
    
    @Column(name = "dTransact")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dTransact;
    
    @Column(name = "sClientID")
    private String sClientID;
    
    @Column(name = "sReferNox")
    private String sReferNox;
    
    @Column(name = "sRemarksx")
    private String sRemarksx;
    
    @Column(name = "sSalesman")
    private String sSalesman;
    
    @Column(name = "cPaymForm")
    private String cPaymForm;
    
    @Column(name = "nTranTotl")
    private Number nTranTotl;
    
    @Column(name = "nVATRatex")
    private Number nVATRatex;
    
    @Column(name = "nDiscount")
    private Number nDiscount;
    
    @Column(name = "nAddDiscx")
    private Number nAddDiscx;
     
    @Column(name = "nFreightx")
    private Number nFreightx;
    
    @Column(name = "nAmtPaidx")
    private Number nAmtPaidx;
    
    @Column(name = "sTermCode")
    private String sTermCode;
    
    @Basic(optional = false)
    @Temporal(TemporalType.DATE)
    @Column(name = "dDueDatex")
    private Date dDueDatex;
    
    @Column(name = "sSourceNo")
    private String sSourceNo;
    
    @Column(name = "sSourceCd")
    private String sSourceCd;
    
    @Column(name = "cTranStat")
    private String cTranStat;
    
    @Column(name = "sAprvCode")
    private String sAprvCode;    
    
    @Basic(optional = false)
    @Column(name = "dCreatedx")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dCreatedx;
    
    @Column(name = "sModified")
    private String sModified;
    
    @Basic(optional = false)
    @Column(name = "dModified")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dModified;
    
    LinkedList laColumns = null;
    
    public Sales_Master(){
        laColumns = new LinkedList();
        laColumns.add("sTransNox");
        laColumns.add("sBranchCd");
        laColumns.add("dTransact");
        laColumns.add("sClientID");
        laColumns.add("sReferNox");
        laColumns.add("sRemarksx");
        laColumns.add("sSalesman");
        laColumns.add("cPaymForm");
        laColumns.add("nTranTotl");
        laColumns.add("nVATRatex");
        laColumns.add("nDiscount");
        laColumns.add("nAddDiscx");
        laColumns.add("nFreightx");
        laColumns.add("nAmtPaidx");
        laColumns.add("sTermCode");
        laColumns.add("dDueDatex");
        laColumns.add("sSourceNo");
        laColumns.add("sSourceCd");
        laColumns.add("cTranStat");
        laColumns.add("sAprvCode");
        laColumns.add("dCreatedx");
        laColumns.add("sModified");
        
        sTransNox = "";
        sBranchCd = "";
        sClientID = "";
        sReferNox = "";
        sRemarksx = "";
        sSalesman = "";
        cPaymForm = "";
        nTranTotl = 0.00;
        nVATRatex = 0.00;
        nDiscount = 0.00;
        nAddDiscx = 0.00;
        nFreightx = 0.00;
        nAmtPaidx = 0.00;
        sTermCode = "";
        sSourceNo = "";
        sSourceCd = "";
        cTranStat = "";
        sAprvCode = "";
        sModified = "";
    }
    
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Sales_Master)) return false;
        
        Sales_Master other = (Sales_Master) object;
        
        return !((this.sTransNox == null && other.sTransNox != null) || 
                (this.sTransNox != null && !this.sTransNox.equals(other.sTransNox)));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.sTransNox);
        return hash;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[sTransNox=" + sTransNox + "]";
    }
    
    @Override
    public Object getValue(int fnColumn) {
        switch(fnColumn){
            case 1: return sTransNox;
            case 2: return sBranchCd;
            case 3: return dTransact;
            case 4: return sClientID;
            case 5: return sReferNox;
            case 6: return sRemarksx;
            case 7: return sSalesman;
            case 8: return cPaymForm;
            case 9: return nTranTotl;
            case 10: return nVATRatex;
            case 11: return nDiscount;
            case 12: return nAddDiscx;
            case 13: return nFreightx;
            case 14: return nAmtPaidx;
            case 15: return sTermCode;
            case 16: return dDueDatex;
            case 17: return sSourceNo;
            case 18: return sSourceCd;
            case 19: return cTranStat;
            case 20: return sAprvCode;
            case 21: return dCreatedx;
            case 22: return sModified;
            case 23: return dModified;
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
            case 2: sBranchCd = (String) foValue; break;
            case 3: dTransact = (Date) foValue; break;
            case 4: sClientID = (String) foValue; break;
            case 5: sReferNox = (String) foValue; break;
            case 6: sRemarksx = (String) foValue; break;
            case 7: sSalesman = (String) foValue; break;
            case 8: cPaymForm = (String) foValue; break;
            case 9: nTranTotl = (Number) foValue; break;
            case 10: nVATRatex = (Number) foValue; break;
            case 11: nDiscount = (Number) foValue; break;
            case 12: nAddDiscx = (Number) foValue; break;
            case 13: nFreightx = (Number) foValue; break;
            case 14: nAmtPaidx = (Number) foValue; break;
            case 15: sTermCode = (String) foValue; break;
            case 16: dDueDatex = (Date) foValue; break;
            case 17: sSourceNo = (String) foValue; break;
            case 18: sSourceCd = (String) foValue; break;
            case 19: cTranStat = (String) foValue; break;
            case 20: sAprvCode = (String) foValue; break;
            case 21: dCreatedx = (Date) foValue; break;
            case 22: sModified = (String) foValue; break;
            case 23: dModified = (Date) foValue; break;
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
            if (getColumn(i + 1).substring(0, 1).equals("d")){
                loJSON.put(laColumns.get(i), SQLUtil.dateFormat(getValue(getColumn(i + 1)), SQLUtil.FORMAT_TIMESTAMP));
            } else 
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
