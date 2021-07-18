package org.xersys.benta.bo;

import org.xersys.imbentaryo.bo.Inventory;
import com.mysql.jdbc.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xersys.benta.dto.Sales_Detail;
import org.xersys.benta.dto.Sales_Master;
import org.xersys.benta.dto.Sales_Others;
import org.xersys.lib.dto.Temp_Transactions;
import org.xersys.kumander.contants.SearchEnum;
import org.xersys.kumander.iface.LMasDetTrans;
import org.xersys.kumander.iface.XEntity;
import org.xersys.kumander.iface.XMasDetTrans;
import org.xersys.kumander.iface.XNautilus;
import org.xersys.kumander.util.MiscUtil;
import org.xersys.kumander.util.SQLUtil;
import org.xersys.kumander.contants.EditMode;
import org.xersys.kumander.contants.RecordStatus;
import org.xersys.kumander.contants.TransactionStatus;
import org.xersys.kumander.util.CommonUtil;

public class Sales implements XMasDetTrans{
    private final String SOURCE_CODE = "Sale";
    
    private XNautilus p_oNautilus;
    private LMasDetTrans p_oListener;
    
    private Inventory p_oInventory;
    
    private boolean p_bSaveToDisk;
    private boolean p_bWithParent;
    
    private String p_sOrderNox;
    private String p_sBranchCd;
    private String p_sMessagex;
    
    private int p_nEditMode;
    private int p_nTranStat;
    
    private Sales_Master p_oMaster;
    private ArrayList<Sales_Detail> p_oDetail;
    private ArrayList<Sales_Others> p_oOthers;
    private ArrayList<Temp_Transactions> p_oTemp;
    
    public Sales(XNautilus foNautilus, String fsBranchCd, boolean fbWithParent){
        p_oNautilus = foNautilus;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;
        p_nEditMode = EditMode.UNKNOWN;
        
        p_oInventory = new Inventory(p_oNautilus);
        loadTempTransactions();
    }
    
    public Sales(XNautilus foNautilus, String fsBranchCd, boolean fbWithParent, int fnTranStat){
        p_oNautilus = foNautilus;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;
        p_nTranStat = fnTranStat;
        p_nEditMode = EditMode.UNKNOWN;
        
        p_oInventory = new Inventory(p_oNautilus);
        loadTempTransactions();
    }

    @Override
    public void setListener(LMasDetTrans foValue) {
        p_oListener = foValue;
    }

    @Override
    public void setSaveToDisk(boolean fbValue) {
        p_bSaveToDisk = fbValue;
    }

    @Override
    public void setMaster(String fsFieldNm, Object foValue) {
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            System.err.println("Transaction is not on update mode.");
            return;
        }
        
        p_oMaster.setValue(fsFieldNm, foValue);
        p_oListener.MasterRetreive(fsFieldNm, p_oMaster.getValue(fsFieldNm));
        
        saveToDisk(RecordStatus.ACTIVE);
    }

    @Override
    public Object getMaster(String fsFieldNm) {
        return p_oMaster.getValue(fsFieldNm);
    }
    
    @Override
    public void setMaster(int fnIndex, Object foValue) {
        setMaster(p_oMaster.getColumn(fnIndex), foValue);
    }

    @Override
    public Object getMaster(int fnIndex) {
        return getMaster(p_oMaster.getColumn(fnIndex));
    }

    @Override
    public void setDetail(int fnRow, String fsFieldNm, Object foValue) {
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            System.err.println("Transaction is not on update mode.");
            return;
        }
        
        switch(fsFieldNm){
            case "sStockIDx":
                loadDetailByCode(fnRow, fsFieldNm, (String) foValue);
                computeTotal();
                p_oListener.MasterRetreive("nTranTotl", p_oMaster.getValue("nTranTotl"));
                return;
        }
        
        p_oDetail.get(fnRow).setValue(fsFieldNm, foValue);
        
        if (fsFieldNm.equals("nQuantity") ||
            fsFieldNm.equals("nUnitPrce") ||
            fsFieldNm.equals("nDiscount") ||
            fsFieldNm.equals("nAddDiscx")){
            
            computeTotal();
            p_oListener.MasterRetreive("nTranTotl", p_oMaster.getValue("nTranTotl"));
        }
        
        saveToDisk(RecordStatus.ACTIVE);
    }

    @Override
    public Object getDetail(int fnRow, String fsFieldNm) {
        return p_oDetail.get(fnRow).getValue(fsFieldNm);
    }
    
    @Override
    public void setDetail(int fnRow, int fnIndex, Object foValue) {
        setDetail(fnRow, p_oDetail.get(0).getColumn(fnIndex), foValue);
    }

    @Override
    public Object getDetail(int fnRow, int fnIndex) {        
        switch (fnIndex){
            case 100:
                return p_oOthers.get(fnRow).getBarCode();
            case 101:
                return p_oOthers.get(fnRow).getDescript();
            case 102:
                return p_oOthers.get(fnRow).getOtherInfo();
            case 103:
                return p_oOthers.get(fnRow).getQtyOnHand();
            default:
                return getDetail(fnRow, p_oDetail.get(0).getColumn(fnIndex));
        }
    }

    @Override
    public String getMessage() {
        return p_sMessagex;
    }
    
    @Override
    public int getEditMode() {
        return p_nEditMode;
    }
    
    @Override
    public int getItemCount() {
        return p_oDetail.size();
    }

    @Override
    public boolean addDetail() {
        if (p_oDetail.isEmpty()){
            p_oDetail.add(new Sales_Detail());
            p_oOthers.add(new Sales_Others());
        }else{
            if (!"".equals((String) p_oDetail.get(getItemCount() - 1).getValue("sStockIDx")) &&
                (int) p_oDetail.get(getItemCount() - 1).getValue("nQuantity") != 0){
                p_oDetail.add(new Sales_Detail());
                p_oOthers.add(new Sales_Others());
            }
        }
        
        saveToDisk(RecordStatus.ACTIVE);
        
        return true;
    }

    @Override
    public boolean delDetail(int fnRow) {
        p_oDetail.remove(fnRow);
        p_oOthers.remove(fnRow);
        
        if (p_oDetail.isEmpty()) return addDetail();
        
        saveToDisk(RecordStatus.ACTIVE);
        
        return true;
    }

    @Override
    public JSONObject SearchMaster(String fsFieldNm, Object foValue) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JSONObject SearchDetail(int fnRow, String fsFieldNm, Object foValue) {
        if (fnRow < 0) return null;
        
        switch (fsFieldNm){
            case "sBarCodex":
                return null;
            case "sDescript":
                return null;
            default:
                return null;
        }
    }
    
    @Override
    public JSONObject Search(SearchEnum.Type foType, String fsValue, String fsKey, String fsFilter, int fnMaxRow, boolean fbExact){
        JSONObject loJSON = new JSONObject();
        
        if (p_oInventory == null){
            loJSON.put("result", "error");
            loJSON.put("message", "Inventory object is not set.");
            return loJSON;
        }
        
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            loJSON.put("result", "error");
            loJSON.put("message", "Invalid edit mode detected.");
            return loJSON;        
        }
        
        if (fsValue.isEmpty()){
            loJSON.put("result", "error");
            loJSON.put("message", "Search value must not be empty.");
            return loJSON;        
        }
        
        p_oInventory.Search().setKey(fsKey);
        p_oInventory.Search().setFilter(fsFilter);
        p_oInventory.Search().sethMax(fnMaxRow);
        p_oInventory.Search().setExact(fbExact);

        return p_oInventory.Search().Search(foType, fsValue);
    }

    @Override
    public boolean NewTransaction() {
        System.out.println(this.getClass().getSimpleName() + ".NewTransaction()");
        
        p_sOrderNox = "";
        
        p_oMaster = new Sales_Master();
        p_oDetail = new ArrayList<>();
        p_oOthers = new ArrayList<>();
        
        addDetail();
        saveToDisk(RecordStatus.ACTIVE);
        
        loadTempTransactions();
        p_nEditMode = EditMode.ADDNEW;
        
        return true;
    }
    
    @Override
    public boolean NewTransaction(String fsOrderNox) {
        System.out.println(this.getClass().getSimpleName() + ".NewTransaction(String fsOrderNox)");
        
        if (fsOrderNox.isEmpty()) return NewTransaction();
        
        ResultSet loTran = null;
        boolean lbLoad = false;
        
        try {
            loTran = CommonUtil.getTempOrder(p_oNautilus, SOURCE_CODE, fsOrderNox);
            
            if (loTran.next()){
                lbLoad = toDTO(loTran.getString("sPayloadx"));
            }
        } catch (SQLException ex) {
            setMessage(ex.getMessage());
            lbLoad = false;
        } finally {
            MiscUtil.close(loTran);
        }
        
        p_sOrderNox = fsOrderNox;
        p_nEditMode = EditMode.ADDNEW;
        
        computeTotal();
        loadTempTransactions();
        
        return lbLoad;
    }

    @Override
    public boolean SaveTransaction(boolean fbConfirmed) {
        System.out.println(this.getClass().getSimpleName() + ".SaveTransaction()");
        
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            System.err.println("Transaction is not on update mode.");
            return false;
        }
        
        if (!fbConfirmed){
            saveToDisk(RecordStatus.ACTIVE);
            return true;
        }
        
        String lsSQL = "";
        
        if (!isEntryOK()) return false;
        
        Sales_Master loOldEnt = null;
        Sales_Master loNewEnt = null;

        loNewEnt = (Sales_Master) p_oMaster;

        try {
            if (!p_bWithParent) p_oNautilus.beginTrans();
        
            if ("".equals((String) loNewEnt.getValue("sTransNox"))){ //new record
                Connection loConn = getConnection();

                loNewEnt.setValue("sTransNox", MiscUtil.getNextCode(loNewEnt.getTable(), "sTransNox", true, loConn, p_sBranchCd));

                if (!p_bWithParent) MiscUtil.close(loConn);

                //save the detail
                if (!saveDetail(loNewEnt, true)){
                    if (!p_bWithParent) p_oNautilus.rollbackTrans();
                    return false;
                }

                loNewEnt.setValue("nEntryNox", getItemCount());

                loNewEnt.setValue("sModified", (String) p_oNautilus.getUserInfo("sUserIDxx"));
                loNewEnt.setValue("dModified", p_oNautilus.getServerDate());

                lsSQL = MiscUtil.makeSQL((XEntity) loNewEnt);
            } else { //old record
                loOldEnt = loadMaster((String) loNewEnt.getValue("sTransNox"));
                
                //save the detail
                if (!saveDetail(loNewEnt, false)){
                    if (!p_bWithParent) p_oNautilus.rollbackTrans();
                    return false;
                }
                
                loNewEnt.setValue("nEntryNox", getItemCount());
                loNewEnt.setValue("sModified", (String) p_oNautilus.getUserInfo("sUserIDxx"));
                loNewEnt.setValue("dModified", p_oNautilus.getServerDate());
                
                lsSQL = MiscUtil.makeSQL((XEntity) loNewEnt, (XEntity) loOldEnt, "sTransNox = " + SQLUtil.toSQL((String) loNewEnt.getValue("sTransNox")));
            }
            
            if (lsSQL.equals("")){
                if (!p_bWithParent) p_oNautilus.rollbackTrans();
                
                setMessage("No record to update");
                return false;
            }
            
            if(p_oNautilus.executeUpdate(lsSQL, loNewEnt.getTable(), p_sBranchCd, "") <= 0){
                if(!p_oNautilus.getMessage().isEmpty())
                    setMessage(p_oNautilus.getMessage());
                else
                    setMessage("No record updated");
            } 
            
            saveToDisk(RecordStatus.INACTIVE);

            if (!p_bWithParent) {
                if(!p_oNautilus.getMessage().isEmpty())
                    p_oNautilus.rollbackTrans();
                else
                    p_oNautilus.commitTrans();
            }    
        } catch (SQLException ex) {
            if (!p_bWithParent) p_oNautilus.rollbackTrans();
            
            ex.printStackTrace();
            setMessage(ex.getMessage());
            return false;
        }
        
        p_nEditMode = EditMode.UNKNOWN;
        
        return true;
    }

    @Override
    public boolean SearchTransaction() {
        System.out.println(this.getClass().getSimpleName() + ".SearchTransaction()");
        
        return true;
    }
    
    @Override
    public ArrayList<Temp_Transactions> TempTransactions() {
        return p_oTemp;
    }

    @Override
    public boolean OpenTransaction(String fsTransNox) {
        System.out.println(this.getClass().getSimpleName() + ".OpenTransaction()");
        setMessage("");
        
        try {
            p_oMaster = loadMaster(fsTransNox);
            p_oDetail = loadDetail(fsTransNox);
            
            if (p_oMaster != null) {
                if ((int) p_oMaster.getValue("nEntryNox") != p_oDetail.size()){
                    setMessage("Transaction discrepancy detected.");
                    return false;
                }
                
                p_nEditMode  = EditMode.READY;
                return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        
        return false;
    }

    @Override
    public boolean UpdateTransaction() {
        System.out.println(this.getClass().getSimpleName() + ".UpdateTransaction()");
        
        if (p_nEditMode != EditMode.READY){
            setMessage("No transaction to update.");
            return false;
        }
        
        p_nEditMode = EditMode.UPDATE;
        
        return true;
    }

    @Override
    public boolean CloseTransaction() {
        System.out.println(this.getClass().getSimpleName() + ".CloseTransaction()");
        
        if (p_nEditMode != EditMode.READY){
            setMessage("No transaction to update.");
            return false;
        }
        
        if ((TransactionStatus.STATE_CANCELLED).equals((String) p_oMaster.getValue("cTranStat"))){
            setMessage("Unable to approve cancelled transactons");
            return false;
        }        
        
        if ((TransactionStatus.STATE_POSTED).equals((String) p_oMaster.getValue("cTranStat"))){
            setMessage("Unable to approve posted transactons");
            return false;
        }
        
        if ((TransactionStatus.STATE_CLOSED).equals((String) p_oMaster.getValue("cTranStat"))){
            setMessage("Transaction was already approved.");
            return false;
        }
        
        String lsSQL = "UPDATE " + p_oMaster.getTable() + " SET" +
                            "  cTranStat = " + TransactionStatus.STATE_CLOSED +
                            ", sModified = " + SQLUtil.toSQL((String) p_oNautilus.getUserInfo("sUserIDxx")) +
                            ", dModified= " + SQLUtil.toSQL(p_oNautilus.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL((String) p_oMaster.getValue("sTransNox"));
        
        if (p_oNautilus.executeUpdate(lsSQL, p_oMaster.getTable(), p_sBranchCd, "") <= 0){
            setMessage(p_oNautilus.getMessage());
            return false;
        }
        
        p_nEditMode  = EditMode.UNKNOWN;
        
        return true;
    }

    @Override
    public boolean CancelTransaction() {
        System.out.println(this.getClass().getSimpleName() + ".CancelTransaction()");
        
        if (p_nEditMode != EditMode.READY){
            setMessage("No transaction to update.");
            return false;
        }
        
        if ((TransactionStatus.STATE_CANCELLED).equals((String) p_oMaster.getValue("cTranStat"))){
            setMessage("Transaction was already cancelled.");
            return false;
        }
        
        //todo:
        //  validate user level/approval code here if we will allow them to cancel approved/posted transactions
        
        if ((TransactionStatus.STATE_CLOSED).equals((String) p_oMaster.getValue("cTranStat"))){   
            setMessage("Unable to cancel approved transactions.");
            return false;
        }
        
        if ((TransactionStatus.STATE_POSTED).equals((String) p_oMaster.getValue("cTranStat"))){
            setMessage("Unable to cancel posted transactions.");
            return false;
        }
        
        String lsSQL = "UPDATE " + p_oMaster.getTable() + " SET" +
                            "  cTranStat = " + TransactionStatus.STATE_CANCELLED +
                            ", sModified = " + SQLUtil.toSQL((String) p_oNautilus.getUserInfo("sUserIDxx")) +
                            ", dModified= " + SQLUtil.toSQL(p_oNautilus.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL((String) p_oMaster.getValue("sTransNox"));
        
        if (p_oNautilus.executeUpdate(lsSQL, p_oMaster.getTable(), p_sBranchCd, "") <= 0){
            setMessage(p_oNautilus.getMessage());
            return false;
        }
        
        p_nEditMode  = EditMode.UNKNOWN;
        
        return true;
    }

    @Override
    public boolean DeleteTransaction(String fsTransNox) {
        System.out.println(this.getClass().getSimpleName() + ".DeleteTransaction()");
        
        if (p_nEditMode != EditMode.READY){
            setMessage("No transaction to update.");
            return false;
        }
        
        if (!(TransactionStatus.STATE_OPEN).equals((String) p_oMaster.getValue("cTranStat"))){
            setMessage("Unable to delete already processed transactions.");
            return false;
        }
        
        //todo:
        //  validate user level here
        
        if (!p_bWithParent) p_oNautilus.beginTrans();
        
        String lsSQL = "DELETE FROM " + p_oMaster.getTable() +
                        " WHERE sTransNox = " + SQLUtil.toSQL((String) p_oMaster.getValue("sTransNox"));
        
        if (p_oNautilus.executeUpdate(lsSQL, p_oMaster.getTable(), p_sBranchCd, "") <= 0){
            if (!p_bWithParent) p_oNautilus.rollbackTrans();
            setMessage(p_oNautilus.getMessage());
            return false;
        }
        
        lsSQL = "DELETE FROM " + new Sales_Detail().getTable() +
                " WHERE sTransNox = " + SQLUtil.toSQL((String) p_oMaster.getValue("sTransNox"));
        
        if (p_oNautilus.executeUpdate(lsSQL, new Sales_Detail().getTable(), p_sBranchCd, "") <= 0){
            if (!p_bWithParent) p_oNautilus.rollbackTrans();
            setMessage(p_oNautilus.getMessage());
            return false;
        }
        
        if (!p_bWithParent) p_oNautilus.commitTrans();
        
        p_nEditMode  = EditMode.UNKNOWN;
        
        return true;
    }

    @Override
    public boolean PostTransaction() {
        System.out.println(this.getClass().getSimpleName() + ".PostTransaction()");
        
        if (p_nEditMode != EditMode.READY){
            setMessage("No transaction to update.");
            return false;
        }
        
        if ((TransactionStatus.STATE_CANCELLED).equals((String) p_oMaster.getValue("cTranStat"))){
            setMessage("Unable to post cancelled transactions.");
            return false;
        }
        
        if ((TransactionStatus.STATE_POSTED).equals((String) p_oMaster.getValue("cTranStat"))){
            setMessage("Transaction was already posted.");
            return false;
        }
        
        //todo:
        //  check if user level validation is still needed
        
        String lsSQL = "UPDATE " + p_oMaster.getTable() + " SET" +
                            "  cTranStat = " + TransactionStatus.STATE_POSTED +
                            ", sPostedxx = " + SQLUtil.toSQL((String) p_oNautilus.getUserInfo("sUserIDxx")) +
                            ", dPostedxx = " + SQLUtil.toSQL(p_oNautilus.getServerDate()) +
                            ", sModified = " + SQLUtil.toSQL((String) p_oNautilus.getUserInfo("sUserIDxx")) +
                            ", dModified= " + SQLUtil.toSQL(p_oNautilus.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL((String) p_oMaster.getValue("sTransNox"));
        
        if (p_oNautilus.executeUpdate(lsSQL, p_oMaster.getTable(), p_sBranchCd, "") <= 0){
            setMessage(p_oNautilus.getMessage());
            return false;
        }
        
        p_nEditMode  = EditMode.UNKNOWN;
        
        return true;
    }  
    
    public boolean DeleteTempTransaction(Temp_Transactions foValue) {
        boolean lbSuccess =  CommonUtil.saveTempOrder(p_oNautilus, foValue.getSourceCode(), foValue.getOrderNo(), foValue.getPayload(), "0");
        loadTempTransactions();
        return lbSuccess;
    }
    
    //added methods
    private void setMessage(String fsValue){
        p_sMessagex = fsValue;
    }
    
    private void saveToDisk(String fsRecdStat){
        if (p_bSaveToDisk){
            String lsPayloadx = toJSONString();
            
            if (p_sOrderNox.isEmpty()){
                p_sOrderNox = CommonUtil.getNextReference(p_oNautilus.getConnection().getConnection(), "xxxTempTransactions", "sOrderNox", "sSourceCd = " + SQLUtil.toSQL(SOURCE_CODE));
                CommonUtil.saveTempOrder(p_oNautilus, SOURCE_CODE, p_sOrderNox, lsPayloadx);
            } else
                CommonUtil.saveTempOrder(p_oNautilus, SOURCE_CODE, p_sOrderNox, lsPayloadx, fsRecdStat);
        }
    }
    
    private void loadTempTransactions(){
        String lsSQL = "SELECT * FROM xxxTempTransactions" +
                        " WHERE cRecdStat = '1'" +
                            " AND sSourceCd = " + SQLUtil.toSQL(SOURCE_CODE);
        
        ResultSet loRS = p_oNautilus.executeQuery(lsSQL);
        
        Temp_Transactions loTemp;
        p_oTemp = new ArrayList<>();
        
        try {
            while(loRS.next()){
                loTemp = new Temp_Transactions();
                loTemp.setSourceCode(loRS.getString("sSourceCd"));
                loTemp.setOrderNo(loRS.getString("sOrderNox"));
                loTemp.setDateCreated(SQLUtil.toDate(loRS.getString("dCreatedx"), SQLUtil.FORMAT_TIMESTAMP));
                loTemp.setPayload(loRS.getString("sPayloadx"));
                p_oTemp.add(loTemp);
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        } finally {
            MiscUtil.close(loRS);
        }
    }
    
    private String toJSONString(){
        JSONParser loParser = new JSONParser();
        JSONArray loDetail = new JSONArray();
        JSONArray loOthers = new JSONArray();
        JSONObject loMaster;
        JSONObject loJSON;

        try {
            loMaster = (JSONObject) loParser.parse(p_oMaster.toJSONString());

            for (int lnCtr = 0; lnCtr < p_oDetail.size(); lnCtr++){
                loJSON = (JSONObject) loParser.parse(p_oDetail.get(lnCtr).toJSONString());
                loDetail.add(loJSON);
            }
            
            for (int lnCtr = 0; lnCtr < p_oOthers.size(); lnCtr++){
                loJSON = (JSONObject) loParser.parse(p_oOthers.get(lnCtr).toJSONString());
                loOthers.add(loJSON);
            }

            loJSON = new JSONObject();
            loJSON.put("master", loMaster);
            loJSON.put("detail", loDetail);
            loJSON.put("others", loOthers);
            
            return loJSON.toJSONString();
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        
        return "";
    }
    
    private boolean toDTO(String fsPayloadx){
        boolean lbLoad = false;
        
        if (fsPayloadx.isEmpty()) return lbLoad;
        
        JSONParser loParser = new JSONParser();
        
        JSONObject loJSON;
        JSONObject loMaster;
        JSONArray laDetail;
        JSONArray laOthers;
        
        
        p_oMaster = new Sales_Master();
        p_oDetail = new ArrayList<>();
        p_oOthers = new ArrayList<>();
        
        try {
            loJSON = (JSONObject) loParser.parse(fsPayloadx);
            loMaster = (JSONObject) loJSON.get("master");
            laDetail = (JSONArray) loJSON.get("detail");
            laOthers = (JSONArray) loJSON.get("others");
        
            int lnCtr;
            String key;
            Iterator iterator;
            
            
            for(iterator = loMaster.keySet().iterator(); iterator.hasNext();) {
                key = (String) iterator.next();
                p_oMaster.setValue(key, loMaster.get(key));
            }
            
            JSONObject loDetail;
            Sales_Detail loSalesDet;
            
            for(lnCtr = 0; lnCtr <= laDetail.size()-1; lnCtr++){
                loSalesDet = new Sales_Detail();
                loDetail = (JSONObject) laDetail.get(lnCtr);
                
                for(iterator = loDetail.keySet().iterator(); iterator.hasNext();) {
                    key = (String) iterator.next();
                    loSalesDet.setValue(key, loDetail.get(key));
                }
                p_oDetail.add(loSalesDet);
            }
            
            JSONObject loOthers;
            Sales_Others loSalesOth;
            
            for(lnCtr = 0; lnCtr <= laOthers.size()-1; lnCtr++){
                loSalesOth = new Sales_Others();
                loOthers = (JSONObject) laOthers.get(lnCtr);
                
                for(iterator = loOthers.keySet().iterator(); iterator.hasNext();) {
                    key = (String) iterator.next();
                    loSalesOth.setValue(key, loOthers.get(key));
                }
                p_oOthers.add(loSalesOth);
            }
            
            lbLoad = true;
        } catch (ParseException ex) {
            setMessage(ex.getMessage());
        }
        
        return lbLoad;
    }
    
    private Connection getConnection(){         
        Connection foConn;
        
        if (p_bWithParent){
            foConn = (Connection) p_oNautilus.getConnection().getConnection();
            
            if (foConn == null) foConn = (Connection) p_oNautilus.doConnect();
        } else 
            foConn = (Connection) p_oNautilus.doConnect();
        
        return foConn;
    }
    
    private boolean saveDetail(Sales_Master foMaster, boolean fbNewRecord) throws SQLException{
        ArrayList<Sales_Detail> loDetail = p_oDetail;
        Sales_Detail loOldDet;
        
        int lnCtr;
        int lnRow;
        String lsSQL;
        
        for (lnCtr = 0; lnCtr <= loDetail.size() - 1; lnCtr++){            
            if (!"".equals((String) loDetail.get(lnCtr).getValue("sStockIDx"))){
                //check the unit price of the item
                if (((Number) loDetail.get(lnCtr).getValue("nUnitPrce")).doubleValue() <= 0.00){
                    setMessage("It seems that an item has zero or negative unit price.");
                    return false;
                }
                
                if (fbNewRecord){
                    loDetail.get(lnCtr).setValue("sTransNox", (String) foMaster.getValue("sTransNox"));
                    loDetail.get(lnCtr).setValue("nEntryNox", lnCtr + 1);
                
                    lsSQL = MiscUtil.makeSQL((XEntity) loDetail.get(lnCtr));
                } else {
                    loOldDet = loadDetail((String )foMaster.getValue("sTransNox"), lnCtr + 1);
                    
                    lsSQL = MiscUtil.makeSQL((XEntity) loDetail.get(lnCtr), (XEntity) loOldDet, 
                                                "sTransNox = " + SQLUtil.toSQL((String) loDetail.get(lnCtr).getValue("sTransNox")) + 
                                                    " AND nEntryNox = " + (int) loDetail.get(lnCtr).getValue("nEntryNox"));
                }
                
                if (!lsSQL.equals("")){                    
                    if(p_oNautilus.executeUpdate(lsSQL, loDetail.get(lnCtr).getTable(), p_sBranchCd, "") <= 0){
                        if(!p_oNautilus.getMessage().isEmpty()){                             
                            setMessage(p_oNautilus.getMessage());
                            return false;
                        }
                    }
                } else {
                    setMessage("No record to update.");
                    return false;
                }
            }
        }
        
        lnRow = loadDetail((String) foMaster.getValue("sTransNox")).size();
        //is the new detail is less than the original count then delete the excess old records
        if (lnCtr < lnRow -1){
            for (lnCtr = lnCtr + 1; lnCtr <= lnRow; lnCtr++){
                lsSQL = "DELETE FROM " + new Sales_Detail().getTable() +  
                        " WHERE sTransNox = " + SQLUtil.toSQL((String) foMaster.getValue("sTransNox")) + 
                            " AND nEntryNox = " + lnCtr;
                
                if(p_oNautilus.executeUpdate(lsSQL, new Sales_Detail().getTable(), p_sBranchCd, "") == 0){
                    if(!p_oNautilus.getMessage().isEmpty()) 
                        setMessage(p_oNautilus.getMessage());
                }else {
                    setMessage("No record updated");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private Sales_Detail loadDetail(String fsTransNox, int fnEntryNox) throws SQLException{
        Sales_Detail loObj = new Sales_Detail();
        
        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(new Sales_Detail()), 
                                                    "sTransNox = " + SQLUtil.toSQL(fsTransNox)) + 
                                                    " AND nEntryNox = " + fnEntryNox;
        
        ResultSet loRS = p_oNautilus.executeQuery(lsSQL);
        
        if (!loRS.next()){
            setMessage("No Record Found");
        }else{
            for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                loObj.setValue(lnCol, loRS.getObject(lnCol));
            }
        }      
        
        return loObj;
    }
    
    private ArrayList<Sales_Detail> loadDetail(String fsTransNox) throws SQLException{
        Sales_Detail loOcc = null;        
        
        ArrayList<Sales_Detail> loDetail = new ArrayList<>();
        
        ResultSet loRS = p_oNautilus.executeQuery(MiscUtil.addCondition(MiscUtil.makeSelect(new Sales_Detail()), 
                                                    "sTransNox = " + SQLUtil.toSQL(fsTransNox)));
               
        for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(loRS); lnCtr ++){
            loRS.absolute(lnCtr);
            
            loOcc = new Sales_Detail();
            
            for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                loOcc.setValue(lnCol, loRS.getObject(lnCol));
            }
            loDetail.add(loOcc);
        }
        
        return loDetail;
    }
    
    private Sales_Master loadMaster(String fsTransNox) throws SQLException{
        Sales_Master loMaster = new Sales_Master();        
        ArrayList<Sales_Detail> loDetail = new ArrayList<>();
        
        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(loMaster), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        ResultSet loRS = p_oNautilus.executeQuery(lsSQL);
        
        if (!loRS.next()){
            setMessage("No Record Found");
        } else{
            //load each column to the entity
            for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                loMaster.setValue(lnCol, loRS.getObject(lnCol));
            }
             
            return loMaster;
        }              
    
        return null;
    }
    
    private void computeTotal(){        
        int lnQuantity;
        double lnUnitPrce;
        double lnDiscount;
        double lnAddDiscx;
        double lnDetlTotl;
        
        double lnTranTotal = 0.00;
        
        for (int lnCtr = 0; lnCtr < p_oDetail.size(); lnCtr++){
            lnQuantity = (int) getDetail(lnCtr, "nQuantity");
            lnUnitPrce = ((Number)getDetail(lnCtr, "nUnitPrce")).doubleValue();
            lnDiscount = ((Number)getDetail(lnCtr, "nDiscount")).doubleValue();
            lnAddDiscx = ((Number)getDetail(lnCtr, "nAddDiscx")).doubleValue();
            lnDetlTotl = (lnQuantity * (lnUnitPrce - (lnUnitPrce * lnDiscount))) - lnAddDiscx;
            
            lnTranTotal += lnDetlTotl;
        }
        
        p_oMaster.setValue("nTranTotl", lnTranTotal);
        saveToDisk(RecordStatus.ACTIVE);
    }
    
    private boolean isEntryOK(){
        p_oMaster.setValue("sBranchCd", (String) p_oNautilus.getSysConfig("sBranchCd"));
        p_oMaster.setValue("dTransact", p_oNautilus.getServerDate());
        
        for (int lnCtr = 0; lnCtr <= p_oTemp.size() -1; lnCtr ++){
            if (p_sOrderNox.equals(p_oTemp.get(lnCtr).getOrderNo())){
                p_oMaster.setValue("dCreatedx", p_oTemp.get(lnCtr).getDateCreated());
            }
        }
        
        //todo: add validations here
        return true;
    }
    
    private void loadDetailByCode(int fnRow, String fsFieldNm, String fsValue){
        JSONObject loJSON;
        JSONArray loArray;
        
        switch(fsFieldNm){
            case "sStockIDx":
                loJSON = Search(SearchEnum.Type.searchInvBranchComplex, fsValue, "a.sStockIDx", "", 1, true);
                
                if ("success".equals((String) loJSON.get("result"))){
                    loArray = (JSONArray) loJSON.get("payload");
                    loJSON = (JSONObject) loArray.get(0);
                    
                    //check if the stock id was already exists
                    boolean lbExist = false;
                    
                    for (int lnCtr = 0; lnCtr <= getItemCount() - 1; lnCtr ++){
                        if (((String) p_oDetail.get(lnCtr).getValue("sStockIDx")).equals((String) loJSON.get("sStockIDx"))){
                            fnRow = lnCtr;
                            lbExist = true;
                            break;
                        }
                    }
                    
                    p_oDetail.get(fnRow).setValue("sStockIDx", (String) loJSON.get("sStockIDx"));
                    p_oDetail.get(fnRow).setValue("nInvCostx", (Number) loJSON.get("nUnitPrce"));
                    p_oDetail.get(fnRow).setValue("nUnitPrce", (Number) loJSON.get("nSelPrce1"));
                    p_oDetail.get(fnRow).setValue("nQuantity", (long) (int) p_oDetail.get(fnRow).getValue("nQuantity") + (long) 1);
                
                    p_oOthers.get(fnRow).setBarCode((String) loJSON.get("sBarCodex"));
                    p_oOthers.get(fnRow).setDescript((String) loJSON.get("sDescript"));
                    p_oOthers.get(fnRow).setOtherInfo("Other Info");
                    p_oOthers.get(fnRow).setQtyOnHand(100);
                    
                    if (!lbExist) addDetail();
                }
        }
    }
}
