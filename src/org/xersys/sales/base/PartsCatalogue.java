package org.xersys.sales.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xersys.commander.contants.EditMode;
import org.xersys.commander.iface.LRecordMas;
import org.xersys.commander.iface.XNautilus;
import org.xersys.commander.util.MiscUtil;
import org.xersys.commander.util.SQLUtil;
import org.xersys.parameters.search.ParamSearchF;

public class PartsCatalogue {
    private XNautilus _nautilus;
    private String _message;
    private int _edit_mode;
    
    private LRecordMas _listener;
    
    private String sBrandCde;
    private String sModelCde;
    private String sCategrCd;
    private String sSeriesID;
    private String sSeriesNm;
    private String sModelNme;
    
    private ParamSearchF p_oSeries;
    
    private CachedRowSet _master;
    
    public PartsCatalogue(XNautilus foValue){
        _nautilus = foValue;
        p_oSeries = new ParamSearchF(_nautilus, ParamSearchF.SearchType.searchModelSeries);
    }
    
    public void setListener(LRecordMas foValue){
        _listener = foValue;
    }
    
    public String getMessage(){
        return _message;
    }
    
    public int getFigureCount() {
        try {
            _master.last();
            
            return _master.getRow();
        } catch (SQLException e) {
            e.printStackTrace();
            _message = "Unable to get row count of transactions.";
            return -1;
        }
    }
    
    public Object getFigure(int fnRow, String fsFieldNm) throws SQLException{
        if (fnRow > getFigureCount()) return null;
    
        _master.absolute(fnRow);
        
        return _master.getObject(fsFieldNm);
    }
    
    public void setMaster(String fsIndex, Object foValue) throws SQLException, ParseException{
        switch (fsIndex.toLowerCase()){
            case "sbrandcde":
                if (!sBrandCde.equals((String) foValue)){
                    sBrandCde = (String) foValue;
                    sModelCde = "";
                    sModelNme = "";
                    sSeriesID = "";
                    sSeriesNm = "";
                    
                    _listener.MasterRetreive("sBrandCde", sBrandCde);
                    _listener.MasterRetreive("sModelCde", "");
                    _listener.MasterRetreive("sSeriesID", "");
                }
                break;
            case "smodelcde":
                if (!sModelCde.equals((String) foValue)){
                    sModelCde = (String) foValue;
                    sModelNme = "";
                    sSeriesID = "";
                    sSeriesNm = "";
                    
                    _listener.MasterRetreive("sModelCde", sModelCde);
                }
                break;
            case "sseriesid":
                getMaster("sSeriesID", (String) foValue);
//                sSeriesID = (String) foValue;
//                _listener.MasterRetreive("sSeriesID", sSeriesID);
                break;
            case "scategrcd":
                sCategrCd = (String) foValue;
                _listener.MasterRetreive("sCategrCd", sCategrCd);
                break;
        }
    }
    
    public boolean NewTransaction(){
        String lsProcName = this.getClass().getSimpleName() + ".NewTransaction()";
        
        System.out.println(lsProcName);
        
        _message = "";
        
        if (_nautilus == null){
            _message = "Application driver is not set.";
            return false;
        }
        
        _master = null;
        
        sCategrCd = "";
        sBrandCde = "";
        sModelCde = "";
        sModelNme = "";
        sSeriesID = "";
        sSeriesNm = "";
        
        _edit_mode = EditMode.READY;
        return true;
    }
    
    public boolean LoadFigures(){
        String lsProcName = this.getClass().getSimpleName() + ".LoadCatalogue()";
        
        System.out.println(lsProcName);
        
        _message = "";
        
        if (_edit_mode != EditMode.READY){
            _message = "Invalid edit mode detected.";
            return false;
        }
        
        ResultSet loRS;
        String lsSQL = getSQ_Detail();
        
        if (!sCategrCd.isEmpty()) 
            lsSQL = MiscUtil.addCondition(lsSQL, "sCategrCd = " + SQLUtil.toSQL(sCategrCd));
        
        lsSQL = MiscUtil.addCondition(lsSQL, "sBrandCde = " + SQLUtil.toSQL(sBrandCde));
        lsSQL = MiscUtil.addCondition(lsSQL, "sModelCde = " + SQLUtil.toSQL(sModelCde));
        lsSQL = MiscUtil.addCondition(lsSQL, "sSeriesID = " + SQLUtil.toSQL(sSeriesID));
        
        try {
            loRS = _nautilus.executeQuery(lsSQL);
        
            if (MiscUtil.RecordCount(loRS) == 0) {
                _message = "No record found based on the given criteria.";
                return false;
            }
            
            RowSetFactory factory = RowSetProvider.newFactory();
            _master = factory.createCachedRowSet();
            _master.populate(loRS);
            MiscUtil.close(loRS);
        } catch (SQLException ex) {
            ex.printStackTrace();
            _message = "SQLException on " + lsProcName;
            return false;
        }

        return true;
    }
    
    public JSONArray getFigureParts(int lnRow){
        String lsProcName = this.getClass().getSimpleName() + ".getFigureParts()";
        
        System.out.println(lsProcName);
        
        String lsSQL = getSQ_Parts();
        
        try {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sFigureID = " + SQLUtil.toSQL((String) getFigure(lnRow, "sFigureID")));
            
            ResultSet loRS = _nautilus.executeQuery(lsSQL);

            if (MiscUtil.RecordCount(loRS) == 0) {
                _message = "No record found based on the given criteria.";
                return null;
            }

            JSONArray loArray = MiscUtil.RS2JSON(loRS);
            MiscUtil.close(loRS);

            return loArray;
        } catch (SQLException ex) {
            ex.printStackTrace();
            _message = "SQLException on " + lsProcName;
            return null;
        }
    }
    
    public JSONObject searchSeries(String fsKey, Object foValue, boolean fbExact){
        p_oSeries.setKey(fsKey);
        p_oSeries.setValue(foValue);
        p_oSeries.setExact(fbExact);
        
        return p_oSeries.Search();
    }
    
    public ParamSearchF getSearchSeries(){
        return p_oSeries;
    }
    
    private void getMaster(String fsFieldNm, Object foValue) throws SQLException, ParseException{       
        JSONObject loJSON;
        JSONParser loParser = new JSONParser();
        
        switch(fsFieldNm){
            case "sSeriesID":
                loJSON = searchSeries("a.sSeriesID", foValue, true);
                
                if ("success".equals((String) loJSON.get("result"))){
                    loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);
                    
                    sCategrCd = "";
                    sBrandCde = (String) loJSON.get("sBrandCde");
                    sModelCde = (String) loJSON.get("sModelCde");
                    sModelNme = (String) loJSON.get("sModelNme");
                    sSeriesID = (String) loJSON.get("sSeriesID");
                    sSeriesNm = (String) loJSON.get("xSeriesNm");
                    
                    if (_listener != null) _listener.MasterRetreive("sCategrCd", sCategrCd);
                    if (_listener != null) _listener.MasterRetreive("sBrandCde", sBrandCde);
                    if (_listener != null) _listener.MasterRetreive("sModelCde", sModelNme);
                    if (_listener != null) _listener.MasterRetreive("sSeriesID", sSeriesNm);
                }
                break;
        }
    }
    
    private String getSQ_Detail(){
        return "SELECT" +
                    "  sFigureID" +
                    ", sDescript" +
                    ", sImageNme" +
                    ", sCategrCd" +
                    ", sModelCde" +
                    ", sBrandCde" +
                    ", sSeriesID" +
                " FROM Catalog_Figures" +
                " ORDER BY sFigureID";
    }
    
    private String getSQ_Parts(){
        return "SELECT" +
                    "  a.sFigureID" +
                    ", a.nEntryNox" +
                    ", a.sStockIDx" +
                    ", a.nQuantity" +
                    ", IFNULL(c.sBarCodex, '') sBarCodex" +
                    ", IFNULL(c.sDescript, '') sDescript" +
                    ", IFNULL(b.nQtyOnHnd, '') nQtyOnHnd" +
                    ", IFNULL(e.sDescript, '') sSeriesDs" +
                    ", IFNULL(c.nSelPrce1, 0.00) nUnitPrce" +
                " FROM Catalog_Parts a" +
                    " LEFT JOIN Inv_Master b" +
                        " LEFT JOIN Inventory c" +
                            " ON b.sStockIDx = c.sStockIDx" +
                    " ON a.sStockIDx = b.sStockIDx" +
                        " AND b.sBranchCd = " + SQLUtil.toSQL((String) _nautilus.getBranchConfig("sBranchCd")) +
                    " LEFT JOIN Catalog_Figures d" +
                        " LEFT JOIN Model_Series e" +
                            " ON d.sSeriesID = e.sSeriesID" +
                        " ON a.sFigureID = d.sFigureID" +
                " ORDER BY nEntryNox";
    }
}
