package org.xersys.sales.base;

import com.mysql.jdbc.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xersys.clients.search.ClientSearch;
import org.xersys.commander.contants.AccessLevel;
import org.xersys.commander.contants.EditMode;
import org.xersys.commander.contants.RecordStatus;
import org.xersys.commander.contants.TransactionStatus;
import org.xersys.commander.contants.UserLevel;
import org.xersys.commander.iface.LApproval;
import org.xersys.commander.iface.LMasDetTrans;
import org.xersys.commander.iface.XMasDetTrans;
import org.xersys.commander.iface.XNautilus;
import org.xersys.commander.util.CommonUtil;
import org.xersys.commander.util.MiscUtil;
import org.xersys.commander.util.SQLUtil;
import org.xersys.commander.util.StringUtil;
import org.xersys.inventory.base.InvTrans;
import org.xersys.inventory.search.InvSearchF;
import org.xersys.lib.pojo.Temp_Transactions;
import org.xersys.purchasing.base.PurchaseOrder;
import org.xersys.purchasing.search.PurchasingSearch;
import org.xersys.sales.search.SalesSearch;

public class SP_Sales implements XMasDetTrans{
    private final String SOURCE_CODE = "SO";
    private final String MASTER_TABLE = "SP_Sales_Master";
    private final String DETAIL_TABLE = "SP_Sales_Detail";
    
    private XNautilus p_oNautilus;
    private LMasDetTrans p_oListener;
    private LApproval p_oApproval;
    
    private boolean p_bSaveToDisk;
    private boolean p_bWithParent;
    
    private String p_sOrderNox;
    private String p_sBranchCd;
    private String p_sMessagex;
    
    private int p_nEditMode;
    private int p_nTranStat;
    
    private CachedRowSet p_oMaster;
    private CachedRowSet p_oDetail;
    
    private ArrayList<Temp_Transactions> p_oTemp;
    
    private InvSearchF p_oSearchItem;
    private ClientSearch p_oSearchClient;
    private ClientSearch p_oSearchSalesman;
    private SalesSearch p_oSearchCO;

    public SP_Sales(XNautilus foNautilus, String fsBranchCd, boolean fbWithParent){
        p_oNautilus = foNautilus;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;
        p_nEditMode = EditMode.UNKNOWN;
        
        p_oSearchItem = new InvSearchF(p_oNautilus, InvSearchF.SearchType.searchBranchStocks);
        p_oSearchClient = new ClientSearch(p_oNautilus, ClientSearch.SearchType.searchClient);
        p_oSearchSalesman = new ClientSearch(p_oNautilus, ClientSearch.SearchType.searchEmployee);
        p_oSearchCO = new SalesSearch(p_oNautilus, SalesSearch.SearchType.searchCustomerOrder);
        
        loadTempTransactions();
    }
    
    public SP_Sales(XNautilus foNautilus, String fsBranchCd, boolean fbWithParent, int fnTranStat){
        p_oNautilus = foNautilus;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;
        p_nTranStat = fnTranStat;
        p_nEditMode = EditMode.UNKNOWN;

        loadTempTransactions();
    }
    
    @Override
    public void setListener(LMasDetTrans foValue) {
        p_oListener = foValue;
    }
    
    public void setApprvListener(LApproval foValue){
        p_oApproval = foValue;
    }

    @Override
    public void setSaveToDisk(boolean fbValue) {
        p_bSaveToDisk = fbValue;
    }
    
    public void setTranStat(int fnValue){
        p_nTranStat = fnValue;
    }

    @Override
    public void setMaster(int fnIndex, Object foValue) {
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            System.err.println("Transaction is not on update mode.");
            return;
        }
        
        try {
            p_oMaster.first();
            
            switch (fnIndex){
                case 5: //sClientID
                    getClient("a.sClientID", foValue);
                    return;
                case 18: //sSourceNo
                    getCustomerOrder("a.sTransNox", foValue);
                    break;
                case 7: //sSalesman
                    getSalesman("a.sClientID", foValue);
                    return;
                case 3: //dTransact
                case 22: //dCreatedx
                    if (StringUtil.isDate(String.valueOf(foValue), SQLUtil.FORMAT_TIMESTAMP))
                        p_oMaster.setObject(fnIndex, foValue);
                    else 
                        p_oMaster.setObject(fnIndex, p_oNautilus.getServerDate());
                    
                    p_oMaster.updateRow();
                    break;
                case 16: //dDueDatex
                    if (StringUtil.isDate(String.valueOf(foValue), SQLUtil.FORMAT_SHORT_DATE))
                        p_oMaster.setObject(fnIndex, foValue);
                    else 
                        p_oMaster.setObject(fnIndex, p_oNautilus.getServerDate());
                    
                    p_oMaster.updateRow();
                    break;
                case 8: //nTranTotl
                case 9: //nVATAmtxx
                case 10: //nDiscount
                case 11: //nAddDiscx
                case 12: //nFreightx
                case 13: //nOthChrge
                case 14: //dDeductnx
                case 15: //nAmtPaidx
                    if (StringUtil.isNumeric(String.valueOf(foValue)))
                        p_oMaster.updateObject(fnIndex, foValue);
                    else
                        p_oMaster.updateObject(fnIndex, 0.00);
                    
                    p_oMaster.updateRow();
                    break;
                default:
                    p_oMaster.updateObject(fnIndex, foValue);
                    p_oMaster.updateRow();
            }
            
            if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, getMaster(fnIndex));
             
            saveToDisk(RecordStatus.ACTIVE, "");
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
        } catch (ParseException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
    }
    
    @Override
    public void setMaster(String fsFieldNm, Object foValue) {
        try {
            setMaster(MiscUtil.getColumnIndex(p_oMaster, fsFieldNm), foValue);
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
        }
    }

    @Override
    public Object getMaster(String fsFieldNm) {
        try {
            p_oMaster.first();
            
            return getMaster(MiscUtil.getColumnIndex(p_oMaster, fsFieldNm));
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
        }
        
        return null;
    }

    @Override
    public Object getMaster(int fnIndex) {
        try {
            p_oMaster.first();
            
            return p_oMaster.getObject(fnIndex);
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
        }
        
        return null;
    }

    @Override
    public void setDetail(int fnRow, String fsFieldNm, Object foValue) {
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            System.err.println("Transaction is not on update mode.");
            return;
        }
        
        try {
            switch (fsFieldNm){
                case "sStockIDx":                     
                    getDetail(fnRow, "a.sStockIDx", foValue);
                    computeTotal();
                    
                    p_oMaster.first();
                    if (p_oListener != null) p_oListener.MasterRetreive("nTranTotl", p_oMaster.getObject("nTranTotl"));
                    break;
                default:
                    p_oDetail.absolute(fnRow + 1);
                    p_oDetail.updateObject(fsFieldNm, foValue);
                    p_oDetail.updateRow();
                    
                    computeTotal();
                    if (p_oListener != null) p_oListener.DetailRetreive(fnRow, fsFieldNm, "");
            }
            
            saveToDisk(RecordStatus.ACTIVE, "");            
        } catch (SQLException | ParseException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
        }
    }

    @Override
    public Object getDetail(int fnRow, String fsFieldNm) {
        try {
            p_oDetail.absolute(fnRow + 1);            
            return p_oDetail.getObject(fsFieldNm);
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
            return null;
        }
    }

    @Override
    public void setDetail(int fnRow, int fnIndex, Object foValue) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getDetail(int fnRow, int fnIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        try {
            p_oDetail.last();
            return p_oDetail.getRow();
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
            return -1;
        }
    }

    @Override
    public boolean addDetail() {
        try {
            if (getItemCount() > 0) {
                if ("".equals((String) getDetail(getItemCount() - 1, "sStockIDx"))){
                    saveToDisk(RecordStatus.ACTIVE, "");
                    return true;
                }
            }
            
            p_oDetail.last();
            p_oDetail.moveToInsertRow();

            MiscUtil.initRowSet(p_oDetail);

            p_oDetail.insertRow();
            p_oDetail.moveToCurrentRow();
        } catch (SQLException e) {
            setMessage(e.getMessage());
            return false;
        }
        
        saveToDisk(RecordStatus.ACTIVE, "");
        return true;
    }

    @Override
    public boolean delDetail(int fnRow) {
        try {
            p_oDetail.absolute(fnRow + 1);
            p_oDetail.deleteRow();
            
            return addDetail();
        } catch (SQLException e) {
            setMessage(e.getMessage());
            return false;
        }
    }

    @Override
    public boolean NewTransaction() {
        System.out.println(this.getClass().getSimpleName() + ".NewTransaction()");
        
        p_sOrderNox = "";
        
        try {
            String lsSQL;
            ResultSet loRS;
            
            RowSetFactory factory = RowSetProvider.newFactory();
            
            //create empty master record
            lsSQL = MiscUtil.addCondition(getSQ_Master(), "0=1");
            loRS = p_oNautilus.executeQuery(lsSQL);
            p_oMaster = factory.createCachedRowSet();
            p_oMaster.populate(loRS);
            MiscUtil.close(loRS);
            addMasterRow();
            
            //create empty detail record
            lsSQL = MiscUtil.addCondition(getSQ_Detail(), "0=1");
            loRS = p_oNautilus.executeQuery(lsSQL);
            p_oDetail = factory.createCachedRowSet();
            p_oDetail.populate(loRS);
            MiscUtil.close(loRS);
            addDetail();
        } catch (SQLException ex) {
            setMessage(ex.getMessage());
            return false;
        }
        
        p_nEditMode = EditMode.ADDNEW;
        
        saveToDisk(RecordStatus.ACTIVE, "");
        loadTempTransactions();
        
        return true;
    }

    @Override
    public boolean NewTransaction(String fsOrderNox) {
        System.out.println(this.getClass().getSimpleName() + ".NewTransaction(String fsOrderNox)");
        
        if (fsOrderNox.isEmpty()) return NewTransaction();
        
        p_sOrderNox = fsOrderNox;
        
        ResultSet loTran = null;
        boolean lbLoad = false;
        
        try {
            loTran = CommonUtil.getTempOrder(p_oNautilus, SOURCE_CODE, fsOrderNox);
            
            if (loTran.next()){
                lbLoad = toDTO(loTran.getString("sPayloadx"));
            }
            
            refreshOnHand();
            computeTotal();
        } catch (SQLException | ParseException ex) {
            setMessage(ex.getMessage());
            lbLoad = false;
        } finally {
            MiscUtil.close(loTran);
        }
        
        p_nEditMode = EditMode.ADDNEW;
        
        loadTempTransactions();
        
        return lbLoad;
    }

    @Override
    public boolean SaveTransaction(boolean fbConfirmed) {
        System.out.println(this.getClass().getSimpleName() + ".SaveTransaction()");
        
        setMessage("");
        
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            System.err.println("Transaction is not on update mode.");
            return false;
        }
        
        if (!fbConfirmed){
            saveToDisk(RecordStatus.ACTIVE, "");
            return true;
        }        
        
        if (!isEntryOK()) return false;
        
        try {
            String lsSQL = "";
            
            if (!p_bWithParent) p_oNautilus.beginTrans();
        
            if ("".equals((String) getMaster("sTransNox"))){ //new record
                Connection loConn = getConnection();

                p_oMaster.updateObject("sTransNox", MiscUtil.getNextCode("SP_Sales_Master", "sTransNox", true, loConn, p_sBranchCd));
                p_oMaster.updateObject("dModified", p_oNautilus.getServerDate());
                p_oMaster.updateRow();
                
                if (!p_bWithParent) MiscUtil.close(loConn);
                
                //save detail
                int lnCtr = 1;
                p_oDetail.beforeFirst();
                while (p_oDetail.next()){
                    if (!"".equals((String) p_oDetail.getObject("sStockIDx"))){
                        p_oDetail.updateObject("sTransNox", p_oMaster.getObject("sTransNox"));
                        p_oDetail.updateObject("nEntryNox", lnCtr);
                    
                        lsSQL = MiscUtil.rowset2SQL(p_oDetail, "SP_Sales_Detail", "sBarCodex;sDescript;nSelPrce1;nQtyOnHnd;sBrandCde;sModelCde;sColorCde");

                        if(p_oNautilus.executeUpdate(lsSQL, "SP_Sales_Detail", p_sBranchCd, "") <= 0){
                            if(!p_oNautilus.getMessage().isEmpty())
                                setMessage(p_oNautilus.getMessage());
                            else
                                setMessage("Unable to update order detail info.");

                            if (!p_bWithParent) p_oNautilus.rollbackTrans();
                            return false;
                        }                         
                        lnCtr++;
                    }
                }
                
                //issue customer order
                if (!issueOrder()){
                    if (!p_bWithParent) p_oNautilus.rollbackTrans();
                    return false;
                }
                
                lsSQL = MiscUtil.rowset2SQL(p_oMaster, "SP_Sales_Master", "sClientNm;xSalesman;xInvNumbr;xAddressx");
            } else { //old record
            }
            
            if (lsSQL.equals("")){
                if (!p_bWithParent) p_oNautilus.rollbackTrans();
                
                setMessage("No record to update.");
                return false;
            }
            
            if (p_oNautilus.executeUpdate(lsSQL, "SP_Sales_Master", p_sBranchCd, "") <= 0){
                if(!p_oNautilus.getMessage().isEmpty())
                    setMessage(p_oNautilus.getMessage());
                else
                    setMessage("Unable to update order master info.");
            }
            
            //update transaction source status
            if (!updateSource()){
                if (!p_bWithParent) p_oNautilus.rollbackTrans();
                return false;
            }
            
            //save inventory transactios
            if (!saveInvTrans()){
                if (!p_bWithParent) p_oNautilus.rollbackTrans();
                return false;
            }
            
            saveToDisk(RecordStatus.INACTIVE, (String) p_oMaster.getObject("sTransNox"));

            if (!p_bWithParent) {
                if(!getMessage().isEmpty())
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
        
        loadTempTransactions();
        p_nEditMode = EditMode.READY;
        
        return true;
    }

    @Override
    public boolean SearchTransaction() {
        System.out.println(this.getClass().getSimpleName() + ".SearchTransaction()");        
        return true;
    }

    @Override
    public boolean OpenTransaction(String fsTransNox) {
        System.out.println(this.getClass().getSimpleName() + ".OpenTransaction()");
        setMessage("");
        
        try {
            String lsSQL;
            ResultSet loRS;
            
            RowSetFactory factory = RowSetProvider.newFactory();
            
            //open master record
            lsSQL = MiscUtil.addCondition(getSQ_Master(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
            loRS = p_oNautilus.executeQuery(lsSQL);
            p_oMaster = factory.createCachedRowSet();
            p_oMaster.populate(loRS);
            MiscUtil.close(loRS);
            
            //open detailo record
            lsSQL = MiscUtil.addCondition(getSQ_Detail(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
            loRS = p_oNautilus.executeQuery(lsSQL);
            p_oDetail = factory.createCachedRowSet();
            p_oDetail.populate(loRS);
            MiscUtil.close(loRS);
            
            if (p_oMaster.size() == 1) {                
                p_nEditMode  = EditMode.READY;
                return true;
            }
            
            setMessage("No transction loaded.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        
        p_nEditMode  = EditMode.UNKNOWN;
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
        
        try {
            if (p_nEditMode != EditMode.READY){
                setMessage("No transaction to update.");
                return false;
            }

            if ((TransactionStatus.STATE_CANCELLED).equals((String) p_oMaster.getObject("cTranStat"))){
                setMessage("Unable to approve cancelled transactons");
                return false;
            }        

            if ((TransactionStatus.STATE_POSTED).equals((String) p_oMaster.getObject("cTranStat"))){
                setMessage("Unable to approve posted transactons");
                return false;
            }

            if ((TransactionStatus.STATE_CLOSED).equals((String) p_oMaster.getObject("cTranStat"))){
                setMessage("Transaction was already approved.");
                return false;
            }

            String lsSQL = "UPDATE " + MASTER_TABLE+ " SET" +
                                "  cTranStat = " + TransactionStatus.STATE_CLOSED +
                                ", dModified= " + SQLUtil.toSQL(p_oNautilus.getServerDate()) +
                            " WHERE sTransNox = " + SQLUtil.toSQL((String) p_oMaster.getObject("sTransNox"));

            if (p_oNautilus.executeUpdate(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
                setMessage(p_oNautilus.getMessage());
                return false;
            }

            p_nEditMode  = EditMode.UNKNOWN;

            return true; 
        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        
        return false;   
    }

    @Override
    public boolean CancelTransaction() {
        System.out.println(this.getClass().getSimpleName() + ".CancelTransaction()");
        
        try {
            if (p_nEditMode != EditMode.READY){
                setMessage("No transaction to update.");
                return false;
            }

            if ((TransactionStatus.STATE_CANCELLED).equals((String) p_oMaster.getObject("cTranStat"))){
                setMessage("Transaction was already cancelled.");
                return false;
            }

            if ((TransactionStatus.STATE_CLOSED).equals((String) p_oMaster.getObject("cTranStat"))){   
                setMessage("Unable to cancel approved transactions.");
                return false;
            }

            if ((TransactionStatus.STATE_POSTED).equals((String) p_oMaster.getObject("cTranStat"))){
                setMessage("Unable to cancel posted transactions.");
                return false;
            }
            
            //check if user is allowed
            if (!p_oNautilus.isUserAuthorized(p_oApproval, 
                    UserLevel.MANAGER + UserLevel.SUPERVISOR + UserLevel.OWNER,
                    AccessLevel.SALES)){
                setMessage(System.getProperty("sMessagex"));
                System.setProperty("sMessagex", "");
                return false;
            }

            String lsSQL = "UPDATE " + MASTER_TABLE+ " SET" +
                                "  cTranStat = " + TransactionStatus.STATE_CANCELLED +
                                ", dModified= " + SQLUtil.toSQL(p_oNautilus.getServerDate()) +
                            " WHERE sTransNox = " + SQLUtil.toSQL((String) p_oMaster.getObject("sTransNox"));

            if (!p_bWithParent) p_oNautilus.beginTrans();
            
            if (p_oNautilus.executeUpdate(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
                setMessage(p_oNautilus.getMessage());
                return false;
            }
            
            //issue customer order
            if (!unissueOrder()){
                if (!p_bWithParent) p_oNautilus.rollbackTrans();
                return false;
            }
            
            if (!unsaveInvTrans()){
                if (!p_bWithParent) p_oNautilus.rollbackTrans();
                return false;
            }
            
            if (!p_bWithParent) p_oNautilus.commitTrans();

            p_nEditMode  = EditMode.UNKNOWN;
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        
        return false;
    }

    @Override
    public boolean DeleteTransaction(String fsTransNox) {
        System.out.println(this.getClass().getSimpleName() + ".DeleteTransaction()");
        
        try {
            if (p_nEditMode != EditMode.READY){
                setMessage("No transaction to update.");
                return false;
            }

            if (!(TransactionStatus.STATE_OPEN).equals((String) p_oMaster.getObject("cTranStat"))){
                setMessage("Unable to delete already processed transactions.");
                return false;
            }

            //check if user is allowed
            if (!p_oNautilus.isUserAuthorized(p_oApproval, 
                    UserLevel.MANAGER + UserLevel.SUPERVISOR + UserLevel.OWNER, 
                    AccessLevel.SALES)){
                setMessage(System.getProperty("sMessagex"));
                System.setProperty("sMessagex", "");
                return false;
            }

            if (!p_bWithParent) p_oNautilus.beginTrans();

            String lsSQL = "DELETE FROM " + MASTER_TABLE +
                            " WHERE sTransNox = " + SQLUtil.toSQL((String) p_oMaster.getObject("sTransNox"));

            if (p_oNautilus.executeUpdate(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
                if (!p_bWithParent) p_oNautilus.rollbackTrans();
                setMessage(p_oNautilus.getMessage());
                return false;
            }

            lsSQL = "DELETE FROM " + p_oDetail.getTableName() +
                    " WHERE sTransNox = " + SQLUtil.toSQL((String) p_oMaster.getObject("sTransNox"));

            if (p_oNautilus.executeUpdate(lsSQL, "SP_Sales_Detail", p_sBranchCd, "") <= 0){
                if (!p_bWithParent) p_oNautilus.rollbackTrans();
                setMessage(p_oNautilus.getMessage());
                return false;
            }

            if (!p_bWithParent) p_oNautilus.commitTrans();

            p_nEditMode  = EditMode.UNKNOWN;

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        
        return false;
    }

    @Override
    public boolean PostTransaction() {
        System.out.println(this.getClass().getSimpleName() + ".PostTransaction()");
        
        try {
            if (p_nEditMode != EditMode.READY){
                setMessage("No transaction to update.");
                return false;
            }

            if ((TransactionStatus.STATE_CANCELLED).equals((String) p_oMaster.getObject("cTranStat"))){
                setMessage("Unable to post cancelled transactions.");
                return false;
            }

            if ((TransactionStatus.STATE_POSTED).equals((String) p_oMaster.getObject("cTranStat"))){
                setMessage("Transaction was already posted.");
                return false;
            }

            //todo:
            //  check if user level validation is still needed

            String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                                "  cTranStat = " + TransactionStatus.STATE_POSTED +
                                ", dModified= " + SQLUtil.toSQL(p_oNautilus.getServerDate()) +
                            " WHERE sTransNox = " + SQLUtil.toSQL((String) p_oMaster.getObject("sTransNox"));

            if (p_oNautilus.executeUpdate(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
                setMessage(p_oNautilus.getMessage());
                return false;
            }

            p_nEditMode  = EditMode.UNKNOWN;

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
            return false;
        }
    }

    @Override
    public ArrayList<Temp_Transactions> TempTransactions() {        
        return p_oTemp;
    }
    
    public JSONObject searchBranchInventory(String fsKey, Object foValue, boolean fbExact){
        p_oSearchItem.setKey(fsKey);
        p_oSearchItem.setValue(foValue);
        p_oSearchItem.setExact(fbExact);
        
        return p_oSearchItem.Search();
    }
    
    public InvSearchF getSearchBranchInventory(){
        return p_oSearchItem;
    }
    
    public JSONObject searchCO(String fsKey, Object foValue, boolean fbExact){
        p_oSearchCO.setKey(fsKey);
        p_oSearchCO.setValue(foValue);
        p_oSearchCO.setExact(fbExact);
        
        p_oSearchCO.addFilter("Status", 10);
        
        return p_oSearchCO.Search();
    }
    
    public SalesSearch getSearchCO(){
        return p_oSearchCO;
    }
    
    public JSONObject searchSalesman(String fsKey, Object foValue, boolean fbExact){
        p_oSearchSalesman.setKey(fsKey);
        p_oSearchSalesman.setValue(foValue);
        p_oSearchSalesman.setExact(fbExact);
        
        return p_oSearchSalesman.Search();
    }
    
    public ClientSearch getSearchSalesman(){
        return p_oSearchSalesman;
    }
    
    public JSONObject searchClient(String fsKey, Object foValue, boolean fbExact){
        p_oSearchClient.setKey(fsKey);
        p_oSearchClient.setValue(foValue);
        p_oSearchClient.setExact(fbExact);
        
        return p_oSearchClient.Search();
    }
    
    public ClientSearch getSearchClient(){
        return p_oSearchClient;
    }
    
    private String getSQ_Master(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.sBranchCd" +
                    ", a.dTransact" +
                    ", a.sReferNox" +
                    ", a.sClientID" +
                    ", a.sRemarksx" +
                    ", a.sSalesman" +
                    ", a.nTranTotl" +
                    ", a.nVATAmtxx" +
                    ", a.nDiscount" +
                    ", a.nAddDiscx" +
                    ", a.nFreightx" +
                    ", a.nOthChrge" +
                    ", a.nDeductnx" +
                    ", a.nAmtPaidx" +
                    ", a.dDueDatex" +
                    ", a.sTermCode" +
                    ", a.sSourceNo" +
                    ", a.sSourceCd" +
                    ", a.cTranStat" +
                    ", a.sApprvlCd" +
                    ", a.dCreatedx" +
                    ", a.dModified" +
                    ", b.sClientNm xClientNm" +
                    ", c.sClientNm xSalesman" +
                    ", IFNULL(g.sClientNm, '') vClientNm" +
                    ", TRIM(CONCAT(IFNULL(d.sHouseNox, ''), ' ', d.sAddressx, ' ', IFNULL(f.sBrgyName, ''), ' ', e.sTownName)) xAddressx" +
                    ", IFNULL(g.sInvNumbr, 'N-O-N-E') xInvNumbr" +
                    ", IFNULL(g.nVATSales, 0.00) + IFNULL(g.nVATAmtxx, 0.00) xAmtPaidx" +
                " FROM " + MASTER_TABLE + " a" +
                    " LEFT JOIN Client_Master b" +
                        " LEFT JOIN Client_Address d ON b.sClientID = d.sClientID" +
                            " AND d.nPriority = 1" +
                        " LEFT JOIN TownCity e ON d.sTownIDxx = e.sTownIDxx" +
                        " LEFT JOIN Barangay f ON d.sBrgyIDxx = f.sBrgyIDxx" +
                    " ON a.sClientID = b.sClientID" +
                    " LEFT JOIN Client_Master c ON a.sSalesman = c.sClientID" +
                    " LEFT JOIN Sales_Invoice g ON a.sTransNox = g.sSourceNo" +
                        " AND g.sSourceCd = 'SO'" +
                        " AND g.cTranStat <> '3'";
    }
    
    private String getSQ_Detail(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.nEntryNox" +	
                    ", a.sOrderNox" +
                    ", a.sStockIDx" +
                    ", a.nQuantity" +
                    ", a.nInvCostx" +	
                    ", a.nUnitPrce" +	
                    ", a.nDiscount" +	
                    ", a.nAddDiscx" +	
                    ", a.sNotesxxx" +
                    ", b.sBarCodex" +
                    ", b.sDescript" +
                    ", b.nSelPrce1" +
                    ", c.nQtyOnHnd" +
                    ", b.sBrandCde" + 
                    ", b.sModelCde" +
                    ", b.sColorCde" +
                " FROM " + DETAIL_TABLE + " a" +
                    " LEFT JOIN Inventory b" +
                        " LEFT JOIN Inv_Master c" +
                            " ON b.sStockIDx = c.sStockIDx" +
                                " AND c.sBranchCd = " + SQLUtil.toSQL(p_sBranchCd) +
                    " ON a.sStockIDx = b.sStockIDx";
    }
    
    private void setMessage(String fsValue){
        p_sMessagex = fsValue;
    }
    
    private void saveToDisk(String fsRecdStat, String fsTransNox){
        if (p_bSaveToDisk && p_nEditMode == EditMode.ADDNEW){
            String lsPayloadx = toJSONString();
            
            if (p_sOrderNox.isEmpty()){
                p_sOrderNox = CommonUtil.getNextReference(p_oNautilus.getConnection().getConnection(), "xxxTempTransactions", "sOrderNox", "sSourceCd = " + SQLUtil.toSQL(SOURCE_CODE));
                CommonUtil.saveTempOrder(p_oNautilus, SOURCE_CODE, p_sOrderNox, lsPayloadx);
            } else
                CommonUtil.saveTempOrder(p_oNautilus, SOURCE_CODE, p_sOrderNox, lsPayloadx, fsRecdStat, fsTransNox);
        }
    }
    
    public void loadTempTransactions(){
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
        JSONArray laMaster = new JSONArray();
        JSONArray laDetail = new JSONArray();
        JSONObject loMaster;
        JSONObject loJSON;

        try {
            String lsValue = MiscUtil.RS2JSONi(p_oMaster).toJSONString();
            laMaster = (JSONArray) loParser.parse(lsValue);
            loMaster = (JSONObject) laMaster.get(0);
            
            lsValue = MiscUtil.RS2JSONi(p_oDetail).toJSONString();
            laDetail = (JSONArray) loParser.parse(lsValue);
 
            loJSON = new JSONObject();
            loJSON.put("master", loMaster);
            loJSON.put("detail", laDetail);
            
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
        
        try {
            String lsSQL;
            ResultSet loRS;
            
            RowSetFactory factory = RowSetProvider.newFactory();
            
            //create empty master record
            lsSQL = MiscUtil.addCondition(getSQ_Master(), "0=1");
            loRS = p_oNautilus.executeQuery(lsSQL);
            p_oMaster = factory.createCachedRowSet();
            p_oMaster.populate(loRS);
            MiscUtil.close(loRS);
            
            //create empty detail record
            lsSQL = MiscUtil.addCondition(getSQ_Detail(), "0=1");
            loRS = p_oNautilus.executeQuery(lsSQL);
            p_oDetail = factory.createCachedRowSet();
            p_oDetail.populate(loRS);
            MiscUtil.close(loRS);
            
            loJSON = (JSONObject) loParser.parse(fsPayloadx);
            loMaster = (JSONObject) loJSON.get("master");
            laDetail = (JSONArray) loJSON.get("detail");
            
            int lnCtr;
            int lnRow;
            
            int lnKey;
            String lsKey;
            String lsIndex;
            Iterator iterator;

            lnRow = 1;
            addMasterRow();
            for(iterator = loMaster.keySet().iterator(); iterator.hasNext();) {
                lsIndex = (String) iterator.next(); //string value of int
                lnKey = Integer.valueOf(lsIndex); //string to in
                lsKey = p_oMaster.getMetaData().getColumnLabel(lnKey); //int to metadata
                p_oMaster.absolute(lnRow);
                if (loMaster.get(lsIndex) != null){
                    switch(lsKey){
                        case "dTransact":
                            p_oMaster.updateObject(lnKey, SQLUtil.toDate((String) loMaster.get(lsIndex), SQLUtil.FORMAT_SHORT_DATE));
                            break;
                        default:
                            p_oMaster.updateObject(lnKey, loMaster.get(lsIndex));
                    }

                    p_oMaster.updateRow();
                }
            }
            
            lnRow = 1;
            for(lnCtr = 0; lnCtr <= laDetail.size()-1; lnCtr++){
                JSONObject loDetail = (JSONObject) laDetail.get(lnCtr);

                addDetail();
                for(iterator = loDetail.keySet().iterator(); iterator.hasNext();) {
                    lsIndex = (String) iterator.next(); //string value of int
                    lnKey = Integer.valueOf(lsIndex); //string to int
                    p_oDetail.absolute(lnRow);
                    p_oDetail.updateObject(lnKey, loDetail.get(lsIndex));
                    p_oDetail.updateRow();
                }
                lnRow++;
            }
        } catch (SQLException | ParseException ex) {
            setMessage(ex.getMessage());
            ex.printStackTrace();
            return false;
        }
        
        return true;
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
    
    private boolean isEntryOK(){
        try {
            //delete the last detail record if stock id
            int lnRow = getItemCount();

            p_oDetail.absolute(lnRow);
            if ("".equals((String) p_oDetail.getObject("sStockIDx"))){
                p_oDetail.deleteRow();
            }
            
            lnRow = getItemCount();

            //validate if there is a detail record
            if (lnRow <= 0) {
                setMessage("There is no item in this transaction");
                addDetail(); //add detail to prevent error on the next attempt of saving
                return false;
            }
            
            refreshOnHand();
            computeTotal();
            
            //check if there is an item with no on hand
            for (int lnCtr = 0; lnCtr <= lnRow -1; lnCtr ++){
                if (Integer.parseInt(String.valueOf(getDetail(lnCtr, "nQtyOnHnd"))) <= 0){
                    setMessage("Order has no inventory on hand.");
                    return false;
                }
                
                if (Integer.parseInt(String.valueOf(getDetail(lnCtr, "nQtyOnHnd"))) <
                    Integer.parseInt(String.valueOf(getDetail(lnCtr, "nQuantity")))){
                    setMessage("Order has less inventory on hand compared to order.");
                    return false;
                }
            }

            //assign values to master record
            p_oMaster.first();
            
            if (p_oMaster.getString("sSalesman").equals("")){
                setMessage("No salesman was assigned.");
                return false;
            }
            
            p_oMaster.updateObject("sBranchCd", (String) p_oNautilus.getBranchConfig("sBranchCd"));
            p_oMaster.updateObject("dTransact", p_oNautilus.getServerDate());

            String lsSQL = "SELECT dCreatedx FROM xxxTempTransactions" +
                            " WHERE sSourceCd = " + SQLUtil.toSQL(SOURCE_CODE) +
                                " AND sOrderNox = " + SQLUtil.toSQL(p_sOrderNox);
            
            ResultSet loRS = p_oNautilus.executeQuery(lsSQL);
            while (loRS.next()){
                p_oMaster.updateObject("dCreatedx", loRS.getString("dCreatedx"));
            }
            
            MiscUtil.close(loRS);
            p_oMaster.updateRow();

            return true;
        } catch (SQLException | ParseException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
            return false;
        }
    }
    
    private void addMasterRow() throws SQLException{
        p_oMaster.last();
        p_oMaster.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oMaster);
        p_oMaster.updateObject("cTranStat", TransactionStatus.STATE_OPEN);
        
        p_oMaster.insertRow();
        p_oMaster.moveToCurrentRow();
    }
    
    private void computeTotal() throws SQLException{        
        int lnQuantity;
        double lnUnitPrce;
        double lnDiscount;
        double lnAddDiscx;
        double lnDetlTotl;
        
        double lnTranTotal = 0.00;
        int lnRow = getItemCount();
        
        for (int lnCtr = 0; lnCtr < lnRow; lnCtr++){
            lnQuantity = Integer.parseInt(String.valueOf(getDetail(lnCtr, "nQuantity")));
            lnUnitPrce = ((Number)getDetail(lnCtr, "nUnitPrce")).doubleValue();
            
            lnDiscount = ((Number)getDetail(lnCtr, "nDiscount")).doubleValue() / 100;
            lnAddDiscx = ((Number)getDetail(lnCtr, "nAddDiscx")).doubleValue();
            
            lnDetlTotl = (lnQuantity * (lnUnitPrce - (lnUnitPrce * lnDiscount))) - (lnQuantity * lnAddDiscx);
            
            lnTranTotal += lnDetlTotl;
        }
        
        p_oMaster.first();
        p_oMaster.updateObject("nTranTotl", lnTranTotal);
        p_oMaster.updateRow();
        
        saveToDisk(RecordStatus.ACTIVE, "");
    }
    
    public boolean DeleteTempTransaction(Temp_Transactions foValue) {
        boolean lbSuccess =  CommonUtil.saveTempOrder(p_oNautilus, foValue.getSourceCode(), foValue.getOrderNo(), foValue.getPayload(), "0");
        loadTempTransactions();
        
        p_nEditMode = EditMode.UNKNOWN;
        return lbSuccess;
    }
    
    private void getDetail(int fnRow, String fsFieldNm, Object foValue) throws SQLException, ParseException{       
        JSONObject loJSON = searchBranchInventory(fsFieldNm, foValue, true);
        JSONParser loParser = new JSONParser();
        
        switch(fsFieldNm){
            case "a.sStockIDx":
                if ("success".equals((String) loJSON.get("result"))){
                    loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);
                    
                    //check if the stock id was already exists
                    boolean lbExist = false;
                    
                    for (int lnCtr = 0; lnCtr <= getItemCount() - 1; lnCtr ++){
                        p_oDetail.absolute(lnCtr + 1);
                        if (((String) p_oDetail.getObject("sStockIDx")).equals((String) loJSON.get("sStockIDx"))){
                            fnRow = lnCtr;
                            lbExist = true;
                            break;
                        }
                    }
                    
                    p_oDetail.absolute(fnRow + 1);
                    p_oDetail.updateObject("sStockIDx", (String) loJSON.get("sStockIDx"));
                    p_oDetail.updateObject("nInvCostx", (Number) loJSON.get("nUnitPrce"));
                    p_oDetail.updateObject("nUnitPrce", (Number) loJSON.get("nSelPrce1"));
                    p_oDetail.updateObject("nQuantity", Integer.parseInt(String.valueOf(p_oDetail.getObject("nQuantity"))) + 1);
                    
                    p_oDetail.updateObject("sBarCodex", (String) loJSON.get("sBarCodex"));
                    p_oDetail.updateObject("sDescript", (String) loJSON.get("sDescript"));
                    p_oDetail.updateObject("nQtyOnHnd", Integer.parseInt(String.valueOf(loJSON.get("nQtyOnHnd"))));
                    p_oDetail.updateObject("nInvCostx", (Number) loJSON.get("nUnitPrce"));
                    p_oDetail.updateObject("sBrandCde", (String) loJSON.get("sBrandCde"));
                    p_oDetail.updateObject("sModelCde", (String) loJSON.get("sModelCde"));
                    p_oDetail.updateObject("sColorCde", (String) loJSON.get("sColorCde"));
                    p_oDetail.updateRow();                    
                    if (!lbExist) addDetail();
                    
                    if (p_oListener != null) p_oListener.DetailRetreive(fnRow, "", "");
                }
        }
    }
    
    private boolean saveInvTrans() throws SQLException{
        InvTrans loTrans = new InvTrans(p_oNautilus, p_sBranchCd);
        int lnRow = getItemCount();
        
        if (loTrans.InitTransaction()){
            p_oMaster.first();
            for (int lnCtr = 0; lnCtr <= lnRow-1; lnCtr++){
                p_oDetail.absolute(lnCtr + 1);
                loTrans.setMaster(lnCtr, "sStockIDx", p_oDetail.getString("sStockIDx"));
                loTrans.setMaster(lnCtr, "nQuantity", p_oDetail.getInt("nQuantity"));
            }
            
            if (!loTrans.Sales(p_oMaster.getString("sTransNox"), 
                                        p_oMaster.getDate("dTransact"), 
                                        EditMode.ADDNEW)){
                setMessage(loTrans.getMessage());
                return false;
            }
            
            return true;
        }
        
        setMessage(loTrans.getMessage());
        return false;
    }
    
    private boolean unsaveInvTrans() throws SQLException{
        InvTrans loTrans = new InvTrans(p_oNautilus, p_sBranchCd);
        int lnRow = getItemCount();
        
        if (loTrans.InitTransaction()){
            p_oMaster.first();
            for (int lnCtr = 0; lnCtr <= lnRow-1; lnCtr++){
                p_oDetail.absolute(lnCtr + 1);
                loTrans.setMaster(lnCtr, "sStockIDx", p_oDetail.getString("sStockIDx"));
                loTrans.setMaster(lnCtr, "nQuantity", p_oDetail.getInt("nQuantity"));
            }
            
            if (!loTrans.Sales(p_oMaster.getString("sTransNox"), 
                                        p_oMaster.getDate("dTransact"), 
                                        EditMode.DELETE)){
                setMessage(loTrans.getMessage());
                return false;
            }
            
            return true;
        }
        
        setMessage(loTrans.getMessage());
        return false;
    }
    
     private void getSalesman(String fsFieldNm, Object foValue) throws SQLException, ParseException{       
        JSONObject loJSON = searchSalesman(fsFieldNm, foValue, true);
        JSONParser loParser = new JSONParser();

        if ("success".equals((String) loJSON.get("result"))){
            loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);

            p_oMaster.first();
            p_oMaster.updateObject(MiscUtil.getColumnIndex(p_oMaster, "sSalesman"), (String) loJSON.get("sClientID"));
            p_oMaster.updateObject(MiscUtil.getColumnIndex(p_oMaster, "xSalesman"), (String) loJSON.get("sClientNm"));
            p_oMaster.updateRow();           
            
            if (p_oListener != null) p_oListener.MasterRetreive("sSalesman", (String) getMaster("xSalesman"));
            saveToDisk(RecordStatus.ACTIVE, "");
        }
    }
     
    private void getCustomerOrder(String fsFieldNm, Object foValue) throws SQLException, ParseException{       
        JSONObject loJSON = searchCO(fsFieldNm, foValue, true);
        JSONParser loParser = new JSONParser();

        if ("success".equals((String) loJSON.get("result"))){
            loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);
            
            SalesOrder loOrder = new SalesOrder(p_oNautilus, p_sBranchCd, true);
            loOrder.setSaveToDisk(false);
            
            if (loOrder.OpenTransaction((String) loJSON.get("sTransNox"))){
                //assign master
                p_oMaster.first();
                p_oMaster.updateObject("sBranchCd", (String) loOrder.getMaster("sBranchCd"));
                p_oMaster.updateObject("sSourceCd", "CO");
                p_oMaster.updateObject("sSourceNo", (String) loOrder.getMaster("sTransNox"));
                p_oMaster.updateObject("nDiscount", Double.valueOf(String.valueOf(loOrder.getMaster("nDiscount"))));
                p_oMaster.updateObject("nAddDiscx", Double.valueOf(String.valueOf(loOrder.getMaster("nAddDiscx"))));
                p_oMaster.updateObject("nFreightx", Double.valueOf(String.valueOf(loOrder.getMaster("nFreightx"))));
                p_oMaster.updateObject("sRemarksx", (String) loOrder.getMaster("sRemarksx"));
                p_oMaster.updateRow();
                
                //create empty detail record
                RowSetFactory factory = RowSetProvider.newFactory();
                String lsSQL = MiscUtil.addCondition(getSQ_Detail(), "0=1");
                ResultSet loRS = p_oNautilus.executeQuery(lsSQL);
                p_oDetail = factory.createCachedRowSet();
                p_oDetail.populate(loRS);
                MiscUtil.close(loRS);
                addDetail();
            
                //assign detail
                int lnRow;
                int lnCtr;
                int lnOrder;
                for (lnCtr = 0; lnCtr <= loOrder.getItemCount()-1; lnCtr++){
                    lnOrder = Integer.parseInt(String.valueOf(loOrder.getDetail(lnCtr, "nReleased"))) -
                                Integer.parseInt(String.valueOf(loOrder.getDetail(lnCtr, "nIssuedxx")));
                    
                    if (lnOrder > 0){
                        lnRow = getItemCount() - 1;
                        setDetail(lnRow, "sStockIDx", (String) loOrder.getDetail(lnCtr, "sStockIDx"));
                        setDetail(lnRow, "sOrderNox", (String) loOrder.getMaster("sTransNox"));
                        setDetail(lnRow, "nQuantity", lnOrder);
                        setDetail(lnRow, "nUnitPrce", Double.valueOf(String.valueOf(loOrder.getDetail(lnCtr, "nUnitPrce"))));
                        setDetail(lnRow, "nDiscount", Double.valueOf(String.valueOf(loOrder.getDetail(lnCtr, "nDiscount"))));
                        setDetail(lnRow, "nAddDiscx", Double.valueOf(String.valueOf(loOrder.getDetail(lnCtr, "nAddDiscx"))));
                    }                    
                }
            }
            
            computeTotal();
            if (p_oListener != null) p_oListener.MasterRetreive("sSourceNo", getMaster("sSourceNo"));
            saveToDisk(RecordStatus.ACTIVE, "");
        }
    }
     
    //get the latest quantity on hand of the items
    private void refreshOnHand() throws SQLException, ParseException{
        JSONObject loJSON;
        JSONParser loParser = new JSONParser();
        
        for (int lnCtr = 0; lnCtr <= getItemCount()-1; lnCtr++){
            p_oDetail.absolute(lnCtr + 1);
            
            if (p_oDetail.getString("sStockIDx").isEmpty()) break;
            
            loJSON = searchBranchInventory("a.sStockIDx", p_oDetail.getString("sStockIDx"), true);
            
            if ("success".equals((String) loJSON.get("result"))){
                loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);
                p_oDetail.updateObject("nQtyOnHnd", Integer.parseInt(String.valueOf(loJSON.get("nQtyOnHnd"))));
                
                //if with order no, do not update the prices
                if (p_oDetail.getString("sOrderNox").isEmpty()){
                    p_oDetail.updateObject("nInvCostx", loJSON.get("nUnitPrce"));
                    p_oDetail.updateObject("nUnitPrce", loJSON.get("nSelPrce1"));
                }
                
                p_oDetail.updateRow();
            }
        }
        
        saveToDisk(RecordStatus.ACTIVE, "");
    }   
    
    private boolean updateSource(){
        String lsSQL = "";
        String lsSourceCd = (String) getMaster("sSourceCd");
        String lsTableNme = "";
        
        if (!"".equals(lsSourceCd)){
            switch (lsSourceCd){
                case "CO":
                    lsTableNme = "SP_Sales_Order_Master";
                    lsSQL = "UPDATE SP_Sales_Order_Master SET" +
                            "  cTranStat = '4'" +
                            " WHERE sTransNox = " + SQLUtil.toSQL((String) getMaster("sSourceNo"));  
                    break;
                default:
                    setMessage("Source is not set for updating status.");
                    return false;
            }
        }
        
        if (!lsSQL.isEmpty()){
            if (p_oNautilus.executeUpdate(lsSQL, lsTableNme, p_sBranchCd, "") <= 0){
                if(!p_oNautilus.getMessage().isEmpty())
                    setMessage(p_oNautilus.getMessage());
                else
                    setMessage("Unable to update order source master info.");

                return false;
            }
        }
        
        return true;
    }
    
    private boolean issueOrder()  throws SQLException{
        String lsSQL;
        
        p_oDetail.beforeFirst();
        while (p_oDetail.next()){
            if (!"".equals((String) p_oDetail.getObject("sStockIDx"))){
                if (!p_oDetail.getString("sOrderNox").isEmpty()){
                    lsSQL = "UPDATE SP_Sales_Order_Detail SET" +
                            "   nIssuedxx = nIssuedxx + " + p_oDetail.getInt("nQuantity") +
                            " WHERE sTransNox = " + SQLUtil.toSQL(p_oDetail.getString("sOrderNox")) +
                                " AND sStockIDx = " + SQLUtil.toSQL(p_oDetail.getString("sStockIDx"));

                    if(p_oNautilus.executeUpdate(lsSQL, "SP_Sales_Order_Detail", p_sBranchCd, "") <= 0){
                        if(!p_oNautilus.getMessage().isEmpty())
                            setMessage(p_oNautilus.getMessage());
                        else
                            setMessage("Unable to update Customer Order.");

                        return false;
                    } 
                    
//                    lsSQL = "UPDATE Inv_Master SET" +
//                                "  nResvOrdr = nResvOrdr + " + p_oDetail.getInt("nQuantity") +
//                                ", dModified = " + SQLUtil.toSQL(p_oNautilus.getServerDate()) +
//                            " WHERE sStockIDx = " + SQLUtil.toSQL(p_oDetail.getString("sStockIDx")) +
//                                " AND sBranchCd = " + SQLUtil.toSQL((String) p_oNautilus.getBranchConfig("sBranchCd"));
//                    
//                    if(p_oNautilus.executeUpdate(lsSQL, "Inv_Master", p_sBranchCd, "") <= 0){
//                        if(!p_oNautilus.getMessage().isEmpty())
//                            setMessage(p_oNautilus.getMessage());
//                        else
//                            setMessage("Unable to update Branch Inventory.");
//
//                        return false;
//                    } 
                }
            }
        }
        
        return true;
    }
    
    private boolean unissueOrder() throws SQLException{
        String lsSQL;
        
        p_oDetail.beforeFirst();
        while (p_oDetail.next()){
            if (!"".equals((String) p_oDetail.getObject("sStockIDx"))){
                if (!p_oDetail.getString("sOrderNox").isEmpty()){
                    lsSQL = "UPDATE SP_Sales_Order_Detail SET" +
                            "   nIssuedxx = nIssuedxx - " + p_oDetail.getInt("nQuantity") +
                            " WHERE sTransNox = " + SQLUtil.toSQL(p_oDetail.getString("sOrderNox")) +
                                " AND sStockIDx = " + SQLUtil.toSQL(p_oDetail.getString("sStockIDx"));

                    if(p_oNautilus.executeUpdate(lsSQL, "SP_Sales_Order_Detail", p_sBranchCd, "") <= 0){
                        if(!p_oNautilus.getMessage().isEmpty())
                            setMessage(p_oNautilus.getMessage());
                        else
                            setMessage("Unable to update Customer Order.");

                        return false;
                    } 

//                    lsSQL = "UPDATE Inv_Master SET" +
//                                "  nResvOrdr = nResvOrdr - " + p_oDetail.getInt("nQuantity") +
//                                ", dModified = " + SQLUtil.toSQL(p_oNautilus.getServerDate()) +
//                            " WHERE sStockIDx = " + SQLUtil.toSQL(p_oDetail.getString("sStockIDx")) +
//                                " AND sBranchCd = " + SQLUtil.toSQL((String) p_oNautilus.getBranchConfig("sBranchCd"));
//                    
//                    if(p_oNautilus.executeUpdate(lsSQL, "Inv_Master", p_sBranchCd, "") <= 0){
//                        if(!p_oNautilus.getMessage().isEmpty())
//                            setMessage(p_oNautilus.getMessage());
//                        else
//                            setMessage("Unable to update Branch Inventory.");
//
//                        return false;
//                    } 
                }
            }
        }
        
        return true;
    }
    
    private void getClient(String fsFieldNm, Object foValue) throws SQLException, ParseException{       
        JSONObject loJSON = searchClient(fsFieldNm, foValue, true);
        JSONParser loParser = new JSONParser();

        if ("success".equals((String) loJSON.get("result"))){
            loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);

            p_oMaster.first();
            p_oMaster.updateObject(MiscUtil.getColumnIndex(p_oMaster, "sClientID"), (String) loJSON.get("sClientID"));
            p_oMaster.updateObject(MiscUtil.getColumnIndex(p_oMaster, "xClientNm"), (String) loJSON.get("sClientNm"));
            p_oMaster.updateRow();           
            
            if (p_oListener != null) p_oListener.MasterRetreive("sClientID", (String) getMaster("xClientNm"));
            saveToDisk(RecordStatus.ACTIVE, "");
        }
    }
}
