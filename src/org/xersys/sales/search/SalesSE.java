package org.xersys.sales.search;

import org.json.simple.JSONObject;
import org.xersys.commander.iface.XNautilus;
import org.xersys.commander.iface.XNeoSearch;

public class SalesSE implements XNeoSearch{
    private final int DEFAULT_LIMIT = 50;
    
    private XNautilus _nautilus;
    
    private Object _type;
    
    private String _value;
    private String _key;
    private String _filter;
    private int _max;
    private boolean _exact;
    
    public SalesSE(XNautilus foValue){
        _nautilus = foValue;
        
        _type = null;
        _value = "";
        _key = "";
        _filter = "";
        _max = DEFAULT_LIMIT;
        _exact = false;
    }
    
    @Override
    public void setSearchType(Object foValue){
        _type = foValue;
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

    @Override
    public JSONObject Search(Object foValue) {
        SalesSF _instance = new SalesSF(_nautilus, _key, _filter, _max, _exact);
        
        JSONObject loJSON = null;
        String lsColName;
        
        SalesSF.Type loType = (SalesSF.Type) _type;
        
        if (null != loType)switch (loType) {
            case searchSPSales:
                lsColName = "dTransact»sClientNm»nTranTotl»xPayablex»sTransNox";
                loJSON = _instance.searchItem(loType, (String) foValue, lsColName);
                if ("success".equals((String) loJSON.get("result"))) {
                    loJSON.put("headers", "DT Transact»Client Name»Amount»Payable»Trans. No.");
                    loJSON.put("colname", lsColName);
                }
                break;
            default:
                break;
        }
        
        return loJSON;
    }
}
