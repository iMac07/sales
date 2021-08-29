package org.xersys.sales.search;

import java.sql.ResultSet;
import org.json.simple.JSONObject;
import org.xersys.commander.iface.XNautilus;
import org.xersys.commander.util.MiscUtil;
import org.xersys.commander.util.SQLUtil;

public class SalesSF {
    private XNautilus _nautilus;
    
    private String _key;
    private String _filter;
    private int _max;
    private boolean _exact;
    
    public enum Type{
        searchSPSales,
    }
    
    public SalesSF(XNautilus foNautilus, String fsKey, String fsFilter, int fnMax, boolean fbExact){
        _nautilus = foNautilus;
        _key = fsKey;
        _filter = fsFilter;
        _max = fnMax;
        _exact = fbExact;
    }
    
    public JSONObject searchItem(Enum foType, String fsValue, String fsFields){
        JSONObject loJSON = new JSONObject();
        
        if (_nautilus == null){
            loJSON.put("result", "error");
            loJSON.put("message", "Application driver is not set.");
            return loJSON;
        }
        
        String lsSQL = "";
        
        if (foType == Type.searchSPSales){
            lsSQL = getSQ_SP_Sales();
        }
        
        if (lsSQL.isEmpty()){
            loJSON.put("result", "error");
            loJSON.put("message", "Search query was not set.");
            return loJSON;
        }
        
        //are we searching with an exact value
        if (_exact)
            lsSQL = MiscUtil.addCondition(lsSQL, _key + " = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, _key + " LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
        //add filter on query
        if (!_filter.isEmpty()) lsSQL = MiscUtil.addCondition(lsSQL, _filter);
        
        //add order by and limit on query
        lsSQL = lsSQL + " ORDER BY " + _key + " LIMIT " + _max;
        
        ResultSet loRS = _nautilus.executeQuery(lsSQL);
        
        if (MiscUtil.RecordCount(loRS) <= 0){
            loJSON.put("result", "error");
            loJSON.put("message", "No record found.");
            return loJSON;
        }
        
        loJSON.put("result", "success");
        loJSON.put("payload", MiscUtil.RS2JSON(loRS, fsFields));
        
        //close resultset
        MiscUtil.close(loRS);
        return loJSON;
    }
    
    private String getSQ_SP_Sales(){
        return "SELECT" +
                    "  IFNULL(a.dCreatedx, a.dTransact) dTransact" +
                    ", IFNULL(b.sClientNm, '') sClientNm" +
                    ", FORMAT((a.nTranTotl - ((a.nTranTotl * a.nDiscount / 100) + a.nAddDiscx) + a.nFreightx), 2) nTranTotl" +
                    ", FORMAT((a.nTranTotl - ((a.nTranTotl * a.nDiscount / 100) + a.nAddDiscx) + a.nFreightx - a.nAmtPaidx), 2) xPayablex" +
                    ", a.sTransNox" +
                " FROM SP_Sales_Master a" +
                    " LEFT JOIN Client_Master b" + 
                        " ON a.sSalesman = b.sClientID";
    }
    
}
