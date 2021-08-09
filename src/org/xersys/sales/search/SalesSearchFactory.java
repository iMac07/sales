package org.xersys.sales.search;

import java.sql.ResultSet;
import org.json.simple.JSONObject;
import org.xersys.commander.iface.XNautilus;
import org.xersys.commander.util.MiscUtil;
import org.xersys.commander.util.SQLUtil;


public class SalesSearchFactory{
    private XNautilus _nautilus;
    
    private String _key;
    private String _filter;
    private int _max;
    private boolean _exact;
    
    public SalesSearchFactory(XNautilus foNautilus, String fsKey, String fsFilter, int fnMax, boolean fbExact){
        _nautilus = foNautilus;
        _key = fsKey;
        _filter = fsFilter;
        _max = fnMax;
        _exact = fbExact;
    }
    
    public JSONObject searchSOTransaction(String fsValue, String fsFields){
        JSONObject loJSON = new JSONObject();
        
        if (_nautilus == null){
            loJSON.put("result", "error");
            loJSON.put("message", "Application driver is not set.");
            return loJSON;
        }
        
        String lsSQL = getSQ_Sales();
        
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
    
    private String getSQ_Sales(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.sBranchCd" +
                    ", DATE_FORMAT(a.dTransact, '%M %d, %Y %r') dTransact" +
                    ", a.sReferNox" +
                    ", a.sRemarksx" +
                    ", Round((a.nTranTotl + a.nFreightx) - ((a.nTranTotl * a.nDiscount / 100) + a.nAddDiscx), 2) nTranTotl" +
                    ", a.nAmtPaidx" +
                    ", IFNULL(b.sClientNm, '')" +
                    ", IFNULL(c.sClientNm, '')" +
                " FROM Sales_Master a" +
                    " LEFT JOIN Client_Master b ON a.sClientID = b.sClientID" +
                    " LEFT JOIN Client_Master c ON a.sSalesman = b.sClientID";
    
    }
}
