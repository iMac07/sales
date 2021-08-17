package org.xersys.sales.search;

import org.xersys.commander.iface.XSearch;
import org.json.simple.JSONObject;
import org.xersys.commander.iface.XNautilus;

public class SalesSearchEngine implements XSearch{
    private final int DEFAULT_LIMIT = 50;
    
    private XNautilus _nautilus;
    
    private String _key;
    private String _filter;
    private int _max;
    private boolean _exact;
    
    private SalesSearchFactory _instance;
    
    public enum Type{
        searchSOTransaction
    }
    
    public SalesSearchEngine(XNautilus foValue){
        _nautilus = foValue;
        
        _key = "";
        _filter = "";
        _max = DEFAULT_LIMIT;
        _exact = false;
    }
    
    @Override
    public void setKey(String fsValue) {
        _key = fsValue;
    }

    @Override
    public void setFilter(String fsValue) {
        _filter = fsValue;
    }

    @Override
    public void setMax(int fnValue) {
        _max = fnValue;
    }

    @Override
    public void setExact(boolean fbValue) {
        _exact = fbValue;
    }

    public JSONObject Search(Enum foFactory, Object foValue) {
        _instance = new SalesSearchFactory(_nautilus, _key, _filter, _max, _exact);
        
        JSONObject loJSON = null;
        String lsColName;
        
        if (foFactory == Type.searchSOTransaction){
            lsColName = "dTransact»IFNULL(b.sClientNm, '')»nTranTotl»nAmtPaidx»sTransNox";
            loJSON = _instance.searchSOTransaction((String) foValue, lsColName);
            if ("success".equals((String) loJSON.get("result"))) {
                loJSON.put("headers", "Date»Client Name»Amount»Paid»Trans. No.");
                loJSON.put("colname", lsColName);
            }
        }
        
        return loJSON;
    }
}
