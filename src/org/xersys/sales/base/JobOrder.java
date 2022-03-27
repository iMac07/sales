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
import org.xersys.commander.contants.EditMode;
import org.xersys.commander.contants.RecordStatus;
import org.xersys.commander.contants.TransactionStatus;
import org.xersys.commander.iface.LMasDetTrans;
import org.xersys.commander.iface.LOthTrans;
import org.xersys.commander.iface.XMasDetTrans;
import org.xersys.commander.iface.XNautilus;
import org.xersys.commander.util.CommonUtil;
import org.xersys.commander.util.MiscUtil;
import org.xersys.commander.util.SQLUtil;
import org.xersys.commander.util.StringUtil;
import org.xersys.inventory.search.InvSearchF;
import org.xersys.lib.pojo.Temp_Transactions;
import org.xersys.parameters.search.ParamSearchF;
import org.xersys.sales.search.SalesSearch;

public class JobOrder implements XMasDetTrans{
    private final String SOURCE_CODE = "JO";
    private final String MASTER_TABLE = "Job_Order_Master";
    private final String DETAIL_TABLE = "Job_Order_Detail";
    private final String PARTS_TABLE = "Job_Order_Parts";
    
    private final XNautilus p_oNautilus;
    private LMasDetTrans p_oListener;
    private LOthTrans p_oOthListener;
    
    private boolean p_bSaveToDisk;
    private final boolean p_bWithParent;
    
    private String p_sOrderNox;
    private final String p_sBranchCd;
    private String p_sMessagex;
    
    private int p_nEditMode;
    private int p_nTranStat;
    
    private CachedRowSet p_oMaster;
    private CachedRowSet p_oDetail;
    private CachedRowSet p_oPartsx;
    
    private ArrayList<Temp_Transactions> p_oTemp;
    
    private InvSearchF p_oParts;
    private InvSearchF p_oSerial;
    private ClientSearch p_oClient;
    private ClientSearch p_oMechanic;
    private ClientSearch p_oAdvisor;
    private ParamSearchF p_oMCDealer;
    private ParamSearchF p_oLabor;   
    private ParamSearchF p_oTerm;
    private SalesSearch p_oSearchTrans;
    private SalesSearch p_oEstimate;

    public JobOrder(XNautilus foNautilus, String fsBranchCd, boolean fbWithParent){
        p_oNautilus = foNautilus;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;
        p_nEditMode = EditMode.UNKNOWN;
        
        p_oParts = new InvSearchF(p_oNautilus, InvSearchF.SearchType.searchBranchStocks);
        p_oSerial = new InvSearchF(p_oNautilus, InvSearchF.SearchType.searchMCSerial);
        p_oClient = new ClientSearch(p_oNautilus, ClientSearch.SearchType.searchClient);
        p_oMechanic = new ClientSearch(p_oNautilus, ClientSearch.SearchType.searchMechanic);
        p_oAdvisor = new ClientSearch(p_oNautilus, ClientSearch.SearchType.searchServiceAdvisor);
        p_oMCDealer = new ParamSearchF(p_oNautilus, ParamSearchF.SearchType.searchMCDealer);
        p_oLabor = new ParamSearchF(p_oNautilus, ParamSearchF.SearchType.searchLabor);
        p_oTerm = new ParamSearchF(p_oNautilus, ParamSearchF.SearchType.searchTerm);
        p_oSearchTrans = new SalesSearch(p_oNautilus, SalesSearch.SearchType.searchJobOrder);
        p_oEstimate = new SalesSearch(p_oNautilus, SalesSearch.SearchType.searchJobEstimate);
        
        loadTempTransactions();
    }
    
    public JobOrder(XNautilus foNautilus, String fsBranchCd, boolean fbWithParent, int fnTranStat){
        p_oNautilus = foNautilus;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;
        p_nEditMode = EditMode.UNKNOWN;
        p_nTranStat = fnTranStat;
        
        p_oParts = new InvSearchF(p_oNautilus, InvSearchF.SearchType.searchBranchStocks);
        p_oSerial = new InvSearchF(p_oNautilus, InvSearchF.SearchType.searchMCSerial);
        p_oClient = new ClientSearch(p_oNautilus, ClientSearch.SearchType.searchClient);
        p_oMechanic = new ClientSearch(p_oNautilus, ClientSearch.SearchType.searchMechanic);
        p_oAdvisor = new ClientSearch(p_oNautilus, ClientSearch.SearchType.searchServiceAdvisor);
        p_oMCDealer = new ParamSearchF(p_oNautilus, ParamSearchF.SearchType.searchMCDealer);
        p_oLabor = new ParamSearchF(p_oNautilus, ParamSearchF.SearchType.searchLabor);
        p_oTerm = new ParamSearchF(p_oNautilus, ParamSearchF.SearchType.searchTerm);
        p_oSearchTrans = new SalesSearch(p_oNautilus, SalesSearch.SearchType.searchJobOrder);
        
        loadTempTransactions();
    }
    
    public void setOtherListener(LOthTrans foValue){
        p_oOthListener = foValue;
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
        try {
            setMaster(MiscUtil.getColumnIndex(p_oMaster, fsFieldNm), foValue);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMaster(String fsFieldNm) {
        try {
            return getMaster(MiscUtil.getColumnIndex(p_oMaster, fsFieldNm));
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage("SQL Exception!");
        }
        
        return null;
    }

    @Override
    public void setMaster(int fnIndex, Object foValue){
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            System.err.println("Transaction is not on update mode.");
            return;
        }
        
        try {
            p_oMaster.first();
            
            switch (fnIndex){
                case 2: //"dTransact"
                case 16: //"dStartedx"
                case 17: //"dFinished"
                    if (StringUtil.isDate(String.valueOf(foValue), SQLUtil.FORMAT_TIMESTAMP))
                        p_oMaster.updateObject(fnIndex, foValue);
                    else 
                        p_oMaster.updateObject(fnIndex, null);
                    
                    p_oMaster.updateRow();
                    break;
                case 29: //"dDueDatex"
                    if (StringUtil.isDate(String.valueOf(foValue), SQLUtil.FORMAT_SHORT_DATE))
                        p_oMaster.updateObject(fnIndex, foValue);
                    else 
                        p_oMaster.updateObject(fnIndex, null);
                    
                    p_oMaster.updateRow();
                    break;
                case 18: //"nLabrTotl"
                case 19: //"nPartTotl"
                case 20: //"nTranTotl"
                case 21: //"nLabrPaid"
                case 22: //"nPartPaid"
                case 23: //"nVATRatex"
                case 24: //"nDiscount"
                case 25: //"nAddDiscx"
                case 26: //"nFreightx"
                case 27: //"nAmtPaidx"
                    if (StringUtil.isNumeric(String.valueOf(foValue)))
                        p_oMaster.updateObject(fnIndex, foValue);
                    else
                        p_oMaster.updateObject(fnIndex, 0.00);
                    
                    p_oMaster.updateRow();
                    break;
                case 3: //"sClientID"
                    getMaster("sClientID", (String) foValue);
                    break;
                case 4: //"sSerialID"
                    getMaster("sSerialID", (String) foValue);
                    break;
                case 7: //"sDealerCd"
                    getMaster("sDealerCd", (String) foValue);
                    break;
                case 8: //"sMechanic"
                    getMaster("sMechanic", (String) foValue);
                    break;
                case 9: //"sSrvcAdvs"
                    getMaster("sSrvcAdvs", (String) foValue);
                    break;
                default:
                    p_oMaster.updateObject(fnIndex, foValue);
                    p_oMaster.updateRow();
            }
            
            if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oMaster.getObject(fnIndex));
             
            saveToDisk(RecordStatus.ACTIVE, "");
        } catch (SQLException | ParseException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
        }
    }

    @Override
    public Object getMaster(int fnIndex) {
        try {
            p_oMaster.first();
            return p_oMaster.getObject(fnIndex);
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage("SQL Exception!");
        }
        
        return null;
    }

    @Override
    public void setDetail(int fnRow, String fsFieldNm, Object foValue) {
        try {
            setDetail(fnRow, MiscUtil.getColumnIndex(p_oDetail, fsFieldNm), foValue);
        } catch (SQLException e) {
            e.printStackTrace();;
        }
    }
    
    public void setParts(int fnRow, String fsFieldNm, Object foValue) {
        try {
            setParts(fnRow, MiscUtil.getColumnIndex(p_oPartsx, fsFieldNm), foValue);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void setParts(int fnRow, int fnIndex, Object foValue) {
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            System.err.println("Transaction is not on update mode.");
            return;
        }
        
        try {
            switch (fnIndex){
                case 3: //sStockIDx
                    getParts(fnRow, "sStockIDx", foValue);
                    computeTotal();
                    break;
                case 4: //nQuantity
                    p_oPartsx.absolute(fnRow + 1);
                    if (!StringUtil.isNumeric(String.valueOf(foValue))) 
                        p_oPartsx.updateObject(fnIndex, 0);
                    else
                        p_oPartsx.updateObject(fnIndex, (int) foValue);
                    
                    p_oPartsx.updateRow();
                    computeTotal();
                    break;
                case 5: //nUnitPrce
                case 6: //nDiscount
                case 7: //nAddDiscx
                    p_oPartsx.absolute(fnRow + 1);
                    if (!StringUtil.isNumeric(String.valueOf(foValue))) 
                        p_oPartsx.updateObject(fnIndex, 0.00);
                    else
                        p_oPartsx.updateObject(fnIndex, (double) foValue);
                    
                    p_oPartsx.updateRow();
                    computeTotal();
                    break;
                default:
                    p_oPartsx.absolute(fnRow + 1);
                    p_oPartsx.updateObject(fnIndex, foValue);
                    p_oPartsx.updateRow();
                    
                    computeTotal();
                    if (p_oOthListener != null) p_oOthListener.OthersRetreive(fnRow, fnIndex, "");
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
            return getDetail(fnRow, MiscUtil.getColumnIndex(p_oDetail, fsFieldNm));
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage("SQL Exception!");
            return null;
        }
    }
    
    @Override
    public Object getDetail(int fnRow, int fnIndex) {
        try {
            if (getItemCount() <= 0 || fnRow + 1 <= 0){
                setMessage("Invalid row index!");
                return null;
            }
            
            p_oDetail.absolute(fnRow + 1);            
            return p_oDetail.getObject(fnIndex);
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage("SQL Exception!");
            return null;
        }
    }
    
    public Object getParts(int fnRow, String fsFieldNm) {
        try {
            return getParts(fnRow, MiscUtil.getColumnIndex(p_oPartsx, fsFieldNm));
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
            return null;
        }
    }
    
    public Object getParts(int fnRow, int fnIndex) {
        try {
            p_oPartsx.absolute(fnRow + 1);            
            return p_oPartsx.getObject(fnIndex);
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
            return null;
        }
    }

    @Override
    public void setDetail(int fnRow, int fnIndex, Object foValue) {
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            System.err.println("Transaction is not on update mode.");
            return;
        }
        
        try {
            switch (fnIndex){
                case 3: //"sLaborCde"
                    getDetail(fnRow, "sLaborCde", foValue);
                    computeTotal();
                    break;
                case 4: //"nQuantity"
                    p_oDetail.absolute(fnRow + 1);
                    if (!StringUtil.isNumeric(String.valueOf(foValue))) 
                        p_oDetail.updateObject(fnIndex, 0);
                    else
                        p_oDetail.updateObject(fnIndex, (int) foValue);
                    
                    p_oDetail.updateRow();
                    computeTotal();
                    break;
                case 5: //"nUnitPrce"
                case 6: //"nDiscount"
                case 7: //"nAddDiscx"
                    p_oDetail.absolute(fnRow + 1);
                    if (!StringUtil.isNumeric(String.valueOf(foValue))) 
                        p_oDetail.updateObject(fnIndex, 0.00);
                    else
                        p_oDetail.updateObject(fnIndex, (double) foValue);
                    
                    p_oDetail.updateRow();
                    computeTotal();
                    break;
                default:
                    p_oDetail.absolute(fnRow + 1);
                    p_oDetail.updateObject(fnIndex, foValue);
                    p_oDetail.updateRow();
                    
                    computeTotal();
                    break;
            }
            
            saveToDisk(RecordStatus.ACTIVE, "");            
        } catch (SQLException | ParseException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
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
        try {
            p_oDetail.last();
            return p_oDetail.getRow();
        } catch (SQLException e) {
            e.printStackTrace();
            setMessage(e.getMessage());
            return -1;
        }
    }
    
    public int getPartsCount() {
        try {
            p_oPartsx.last();
            return p_oPartsx.getRow();
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
                if ("".equals((String) getDetail(getItemCount() - 1, "sLaborCde"))){
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
    
    public boolean addParts() {
        try {
            if (getPartsCount() > 0) {
                if ("".equals((String) getParts(getPartsCount() - 1, "sStockIDx"))){
                    saveToDisk(RecordStatus.ACTIVE, "");
                    return true;
                }
            }
            
            p_oPartsx.last();
            p_oPartsx.moveToInsertRow();

            MiscUtil.initRowSet(p_oPartsx);

            p_oPartsx.insertRow();
            p_oPartsx.moveToCurrentRow();
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
    
    public boolean delParts(int fnRow) {
        try {
            p_oPartsx.absolute(fnRow + 1);
            p_oPartsx.deleteRow();
            
            return addParts();
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
            
            lsSQL = MiscUtil.addCondition(getSQ_Parts(), "0=1");
            loRS = p_oNautilus.executeQuery(lsSQL);
            p_oPartsx = factory.createCachedRowSet();
            p_oPartsx.populate(loRS);
            MiscUtil.close(loRS);
            addParts();
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
            
            computeTotal();
        } catch (SQLException ex) {
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
            setMessage("Transaction is not on update mode.");
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

                p_oMaster.updateObject("sTransNox", MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", true, loConn, p_sBranchCd));
                p_oMaster.updateObject("dModified", p_oNautilus.getServerDate());
                p_oMaster.updateRow();
                
                if (!p_bWithParent) MiscUtil.close(loConn);
                
                //save detail
                int lnCtr = 1;
                p_oDetail.beforeFirst();
                while (p_oDetail.next()){
                    if (!"".equals((String) p_oDetail.getObject("sLaborCde"))){
                        p_oDetail.updateObject("sTransNox", p_oMaster.getObject("sTransNox"));
                        p_oDetail.updateObject("nEntryNox", lnCtr);
                    
                        lsSQL = MiscUtil.rowset2SQL(p_oDetail, DETAIL_TABLE, "sLaborNme");

                        if(p_oNautilus.executeUpdate(lsSQL, DETAIL_TABLE, p_sBranchCd, "") <= 0){
                            if(!p_oNautilus.getMessage().isEmpty())
                                setMessage(p_oNautilus.getMessage());
                            else
                                setMessage("No record updated");

                            if (!p_bWithParent) p_oNautilus.rollbackTrans();
                            return false;
                        } 
                        lnCtr++;
                    }
                }
                
                //save parts
                lnCtr = 1;
                p_oPartsx.beforeFirst();
                while (p_oPartsx.next()){
                    if (!"".equals((String) p_oPartsx.getObject("sStockIDx"))){
                        p_oPartsx.updateObject("sTransNox", p_oMaster.getObject("sTransNox"));
                        p_oPartsx.updateObject("nEntryNox", lnCtr);
                    
                        lsSQL = MiscUtil.rowset2SQL(p_oPartsx, PARTS_TABLE, "sBarCodex;sDescript;nSelPrce1;nQtyOnHnd;sBrandCde;sModelCde;sColorCde;");

                        if(p_oNautilus.executeUpdate(lsSQL, PARTS_TABLE, p_sBranchCd, "") <= 0){
                            if(!p_oNautilus.getMessage().isEmpty())
                                setMessage(p_oNautilus.getMessage());
                            else
                                setMessage("No record updated");

                            if (!p_bWithParent) p_oNautilus.rollbackTrans();
                            return false;
                        } 
                        lnCtr++;
                    }
                }
                
                lsSQL = MiscUtil.rowset2SQL(p_oMaster, MASTER_TABLE, "xClientNm;xEngineNo;xFrameNox;xTermName;xSrvcAdvs;xDealerNm;xMechanic");
            } else { //old record
            }
            
            if (lsSQL.equals("")){
                if (!p_bWithParent) p_oNautilus.rollbackTrans();
                
                setMessage("No record to update");
                return false;
            }
            
            if(p_oNautilus.executeUpdate(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
                if(!p_oNautilus.getMessage().isEmpty())
                    setMessage(p_oNautilus.getMessage());
                else
                    setMessage("No record updated");
            } 
            
            saveToDisk(RecordStatus.INACTIVE, (String) p_oMaster.getObject("sTransNox"));

            if (!p_bWithParent) {
                if(!p_oNautilus.getMessage().isEmpty()){
                    p_oNautilus.rollbackTrans();
                    setMessage(p_oNautilus.getMessage());
                    return false;
                } else
                    p_oNautilus.commitTrans();
            }    
        } catch (SQLException ex) {
            if (!p_bWithParent) p_oNautilus.rollbackTrans();
            
            ex.printStackTrace();
            setMessage(ex.getMessage());
            return false;
        }
        
        loadTempTransactions();
        p_nEditMode = EditMode.UNKNOWN;
        
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
            
            //open detail record
            lsSQL = MiscUtil.addCondition(getSQ_Detail(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
            loRS = p_oNautilus.executeQuery(lsSQL);
            p_oDetail = factory.createCachedRowSet();
            p_oDetail.populate(loRS);
            MiscUtil.close(loRS);
            
            //open parts record
            lsSQL = MiscUtil.addCondition(getSQ_Parts(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
            loRS = p_oNautilus.executeQuery(lsSQL);
            p_oPartsx = factory.createCachedRowSet();
            p_oPartsx.populate(loRS);
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

            String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                                "  cTranStat = " + TransactionStatus.STATE_CLOSED +
                                ", dModified = " + SQLUtil.toSQL(p_oNautilus.getServerDate()) +
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

            //todo:
            //  validate user level/approval code here if we will allow them to cancel approved/posted transactions

            if ((TransactionStatus.STATE_CLOSED).equals((String) p_oMaster.getObject("cTranStat"))){   
                setMessage("Unable to cancel approved transactions.");
                return false;
            }

            if ((TransactionStatus.STATE_POSTED).equals((String) p_oMaster.getObject("cTranStat"))){
                setMessage("Unable to cancel posted transactions.");
                return false;
            }

            String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                                "  cTranStat = " + TransactionStatus.STATE_CANCELLED +
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

            //todo:
            //  validate user level here

            if (!p_bWithParent) p_oNautilus.beginTrans();

            String lsSQL = "DELETE FROM " + p_oMaster.getTableName() +
                            " WHERE sTransNox = " + SQLUtil.toSQL((String) p_oMaster.getObject("sTransNox"));

            if (p_oNautilus.executeUpdate(lsSQL, p_oMaster.getTableName(), p_sBranchCd, "") <= 0){
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
    
    public void setTranStat(int fnValue){
        p_nTranStat = fnValue;
    }
    
    public boolean loadEstimate(String fsTransNox){
        if (p_nEditMode != EditMode.ADDNEW){
            setMessage("Invalid Edit Mode Detected.");
            return false;
        }
        
        JobEstimate loEstimate = new JobEstimate(p_oNautilus, p_sBranchCd, true);
        
        if (loEstimate.OpenTransaction(fsTransNox)){
            setMaster("sJobDescr", (String) loEstimate.getMaster("sJobDescr"));
            setMaster("sSourceNo", (String) loEstimate.getMaster("sTransNox"));
            setMaster("sSourceCd", "JEst");
            
            setMaster("sClientID", (String) loEstimate.getMaster("sClientID"));
            setMaster("sSerialID", (String) loEstimate.getMaster("sSerialID"));
            setMaster("sDealerCd", (String) loEstimate.getMaster("sDealerCd"));
            setMaster("sSrvcAdvs", (String) loEstimate.getMaster("sSrvcAdvs"));
            setMaster("sTermCode", (String) loEstimate.getMaster("sTermCode"));

            for (int lnCtr = 0; lnCtr <= loEstimate.getItemCount()-1; lnCtr++){
                setDetail(lnCtr, "sLaborCde", (String) loEstimate.getDetail(lnCtr, "sLaborCde"));
                setDetail(lnCtr, "nQuantity", (int) loEstimate.getDetail(lnCtr, "nQuantity"));
                setDetail(lnCtr, "nUnitPrce", Double.valueOf(loEstimate.getDetail(lnCtr, "nUnitPrce").toString()));
                setDetail(lnCtr, "nDiscount", Double.valueOf(loEstimate.getDetail(lnCtr, "nDiscount").toString()));
                setDetail(lnCtr, "nAddDiscx", Double.valueOf(loEstimate.getDetail(lnCtr, "nAddDiscx").toString()));
                addDetail();
            }
            
            for (int lnCtr = 0; lnCtr <= loEstimate.getPartsCount()-1; lnCtr++){
                setParts(lnCtr, "sStockIDx", (String) loEstimate.getParts(lnCtr, "sStockIDx"));
                setDetail(lnCtr, "nQuantity", (int) loEstimate.getParts(lnCtr, "nQuantity"));
                setDetail(lnCtr, "nUnitPrce", Double.valueOf(loEstimate.getParts(lnCtr, "nUnitPrce").toString()));
                setDetail(lnCtr, "nDiscount", Double.valueOf(loEstimate.getParts(lnCtr, "nDiscount").toString()));
                setDetail(lnCtr, "nAddDiscx", Double.valueOf(loEstimate.getParts(lnCtr, "nAddDiscx").toString()));
                addParts();
            }
        } else {
            setMessage("Unable to load Job Estimate transaction.");
            return false;
        }
        
        return true;
    }
    
    public JSONObject searchTransaction(String fsKey, Object foValue, boolean fbExact){
        p_oSearchTrans.setKey(fsKey);
        p_oSearchTrans.setValue(foValue);
        p_oSearchTrans.setExact(fbExact);
        
        p_oSearchTrans.addFilter("Status", p_nTranStat);
        
        return p_oSearchTrans.Search();
    }
    
    public SalesSearch getSearchTransaction(){
        return p_oSearchTrans;
    }
    
    public JSONObject searchEstimate(String fsKey, Object foValue, boolean fbExact){
        p_oEstimate.setKey(fsKey);
        p_oEstimate.setValue(foValue);
        p_oEstimate.setExact(fbExact);
        
        p_oEstimate.addFilter("Status", 2);
        
        return p_oEstimate.Search();
    }
    
    public SalesSearch getSearchEstimate(){
        return p_oEstimate;
    }
    
    public JSONObject searchParts(String fsKey, Object foValue, boolean fbExact){
        p_oParts.setKey(fsKey);
        p_oParts.setValue(foValue);
        p_oParts.setExact(fbExact);
        
        return p_oParts.Search();
    }
    
    public InvSearchF getSearchParts(){
        return p_oParts;
    }
    
    public JSONObject searchLabor(String fsKey, Object foValue, boolean fbExact){
        p_oLabor.setKey(fsKey);
        p_oLabor.setValue(foValue);
        p_oLabor.setExact(fbExact);
        
        return p_oLabor.Search();
    }
    
    public ParamSearchF getSearchLabor(){
        return p_oLabor;
    }
    
    public JSONObject searchSerial(String fsKey, Object foValue, boolean fbExact){
        p_oSerial.setKey(fsKey);
        p_oSerial.setValue(foValue);
        p_oSerial.setExact(fbExact);
        
        return p_oSerial.Search();
    }
    
    public InvSearchF getSearchSerial(){
        return p_oSerial;
    }
    
    public JSONObject searchClient(String fsKey, Object foValue, boolean fbExact){
        p_oClient.setKey(fsKey);
        p_oClient.setValue(foValue);
        p_oClient.setExact(fbExact);
        
        return p_oClient.Search();
    }
    
    public ClientSearch getSearchClient(){
        return p_oClient;
    }
    
    public JSONObject searchMechanic(String fsKey, Object foValue, boolean fbExact){
        p_oMechanic.setKey(fsKey);
        p_oMechanic.setValue(foValue);
        p_oMechanic.setExact(fbExact);
        
        return p_oMechanic.Search();
    }
    
    public ClientSearch getSearchMechanic(){
        return p_oMechanic;
    }
    
    public JSONObject searchAdvisor(String fsKey, Object foValue, boolean fbExact){
        p_oAdvisor.setKey(fsKey);
        p_oAdvisor.setValue(foValue);
        p_oAdvisor.setExact(fbExact);
        
        return p_oAdvisor.Search();
    }
    
    public ClientSearch getSearchAdvisor(){
        return p_oAdvisor;
    }
    
    public JSONObject searchTerm(String fsKey, Object foValue, boolean fbExact){
        p_oTerm.setKey(fsKey);
        p_oTerm.setValue(foValue);
        p_oTerm.setExact(fbExact);
        
        return p_oTerm.Search();
    }
    
    public ParamSearchF getTerm(){
        return p_oTerm;
    }
    
    public JSONObject searchMCDealer(String fsKey, Object foValue, boolean fbExact){
        p_oMCDealer.setKey(fsKey);
        p_oMCDealer.setValue(foValue);
        p_oMCDealer.setExact(fbExact);
        
        return p_oMCDealer.Search();
    }
    
    public ParamSearchF getSearchMCDealer(){
        return p_oMCDealer;
    }
    
    private String getSQ_Master(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.dTransact" +
                    ", a.sClientID" +
                    ", a.sSerialID" +
                    ", a.sJobDescr" +
                    ", a.nKmReadng" +
                    ", a.sDealerCd" +
                    ", a.sMechanic" +
                    ", a.sSrvcAdvs" +
                    ", a.nContrlNo" +
                    ", a.sWrnCpnNo" +
                    ", a.nWrnCpnNo" +
                    ", a.cWarranty" +
                    ", a.sBckJobNo" +
                    ", a.nBckJobNo" +
                    ", a.dStartedx" +
                    ", a.dFinished" +
                    ", a.nLabrTotl" +
                    ", a.nPartTotl" +
                    ", a.nTranTotl" +
                    ", a.nLabrPaid" +
                    ", a.nPartPaid" +
                    ", a.nVATRatex" +
                    ", a.nDiscount" +
                    ", a.nAddDiscx" +
                    ", a.nFreightx" +
                    ", a.nAmtPaidx" +
                    ", a.sTermCode" +
                    ", a.dDueDatex" +
                    ", a.sSourceNo" +
                    ", a.sSourceCd" +
                    ", a.cTranStat" +
                    ", a.sAprvCode" +
                    ", a.dCreatedx" +
                    ", a.dModified" +
                    ", IFNULL(b.sClientNm, '') xClientNm" +
                    ", IFNULL(c.sSerial01, '') xEngineNo" +
                    ", IFNULL(c.sSerial02, '') xFrameNox" +
                    ", IFNULL(d.sClientNm, '') xMechanic" +
                    ", IFNULL(e.sClientNm, '') xSrvcAdvs" +
                    ", IFNULL(f.sDescript, '') xDealerNm" +
                " FROM Job_Order_Master a" +
                    " LEFT JOIN Client_Master b ON a.sClientID = b.sClientID" +
                    " LEFT JOIN Inv_Serial c ON a.sSerialID = c.sSerialID" +
                    " LEFT JOIN Client_Master d ON a.sMechanic = d.sClientID" +
                    " LEFT JOIN Client_Master e ON a.sSrvcAdvs = e.sClientID" +
                    " LEFT JOIN MC_Dealers f ON a.sDealerCd = f.sDealerCd";
    }
    
    private String getSQ_Detail(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.nEntryNox" +
                    ", a.sLaborCde" +
                    ", a.nQuantity" +
                    ", a.nUnitPrce" +
                    ", a.nDiscount" +
                    ", a.nAddDiscx" +
                    ", a.sNotesxxx" +
                    ", a.dModified" +
                    ", IFNULL(b.sDescript, '') sLaborNme" +
                " FROM Job_Order_Detail a" +
                    " LEFT JOIN Labor b ON a.sLaborCde = b.sLaborCde";
    }
    
    private String getSQ_Parts(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.nEntryNox" +
                    ", a.sStockIDx" +
                    ", a.nQuantity" +
                    ", a.nInvCostx" +
                    ", a.nUnitPrce" +
                    ", a.nDiscount" +
                    ", a.nAddDiscx" +
                    ", a.dModified" +
                    ", b.sBarCodex" +
                    ", b.sDescript" +
                    ", b.nSelPrce1" +
                    ", c.nQtyOnHnd" +
                    ", b.sBrandCde" + 
                    ", b.sModelCde" +
                    ", b.sColorCde" +
                " FROM Job_Order_Parts a" +	
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
        JSONArray laMaster;
        JSONArray laDetail;
        JSONArray laPartsx;
        JSONObject loMaster;
        JSONObject loJSON;

        try {
            String lsValue = MiscUtil.RS2JSONi(p_oMaster).toJSONString();
            laMaster = (JSONArray) loParser.parse(lsValue);
            loMaster = (JSONObject) laMaster.get(0);
            
            lsValue = MiscUtil.RS2JSONi(p_oDetail).toJSONString();
            laDetail = (JSONArray) loParser.parse(lsValue);
            
            lsValue = MiscUtil.RS2JSONi(p_oPartsx).toJSONString();
            laPartsx = (JSONArray) loParser.parse(lsValue);
 
            loJSON = new JSONObject();
            loJSON.put("master", loMaster);
            loJSON.put("detail", laDetail);
            loJSON.put("parts", laPartsx);
            
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
        JSONArray laPartsx;
        
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
            
            //create empty parts record
            lsSQL = MiscUtil.addCondition(getSQ_Parts(), "0=1");
            loRS = p_oNautilus.executeQuery(lsSQL);
            p_oPartsx = factory.createCachedRowSet();
            p_oPartsx.populate(loRS);
            MiscUtil.close(loRS);
            
            loJSON = (JSONObject) loParser.parse(fsPayloadx);
            loMaster = (JSONObject) loJSON.get("master");
            laDetail = (JSONArray) loJSON.get("detail");
            laPartsx = (JSONArray) loJSON.get("parts");
            
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
                        case "dStartedx":
                        case "dFinished":
                        case "dDueDatex":
                            p_oMaster.updateObject(lnKey, SQLUtil.toDate((String) loMaster.get(lsIndex), SQLUtil.FORMAT_TIMESTAMP));
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
            
            lnRow = 1;
            for(lnCtr = 0; lnCtr <= laPartsx.size()-1; lnCtr++){
                JSONObject loDetail = (JSONObject) laPartsx.get(lnCtr);

                addParts();
                for(iterator = loDetail.keySet().iterator(); iterator.hasNext();) {
                    lsIndex = (String) iterator.next(); //string value of int
                    lnKey = Integer.valueOf(lsIndex); //string to int
                    p_oPartsx.absolute(lnRow);
                    p_oPartsx.updateObject(lnKey, loDetail.get(lsIndex));
                    p_oPartsx.updateRow();
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
            if ("".equals((String) getMaster("sJobDescr"))){
                setMessage("No job description was set.");
                return false;
            }
            
            if ("".equals((String) getMaster("sClientID"))){
                setMessage("No client was set.");
                return false;
            }
            
            if ("".equals((String) getMaster("sSerialID"))){
                setMessage("No motorcycle was set.");
                return false;
            }
            
            if ("".equals((String) getMaster("sDealerCd"))){
                setMessage("No MC dealer set.");
                return false;
            }
            
            if ("".equals((String) getMaster("sMechanic"))){
                setMessage("No mechanic was set.");
                return false;
            }
            
            if ("".equals((String) getMaster("sSrvcAdvs"))){
                setMessage("No service advisor was set.");
                return false;
            }
            
            //delete the last detail record if stock id
            int lnCtr = getItemCount();
            
            p_oDetail.absolute(lnCtr);
            if ("".equals((String) p_oDetail.getObject("sLaborCde"))){
                p_oDetail.deleteRow();
            }

            //validate if there is a detail record
            if (getItemCount() <= 0) {
                setMessage("There are no item in this transaction.");
                addDetail(); //add detail to prevent error on the next attempt of saving
                return false;
            }
            
            lnCtr = getPartsCount();
            
            p_oPartsx.absolute(lnCtr);
            if ("".equals((String) p_oPartsx.getObject("sStockIDx"))){
                p_oPartsx.deleteRow();
            }
            
            //assign values to master record
            p_oMaster.first();
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
        } catch (SQLException e) {
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
        double lnLaborTotl = 0.00;
        double lnPartsTotl = 0.00;
        
        int lnRow = getItemCount();
        for (int lnCtr = 0; lnCtr < lnRow; lnCtr++){
            lnQuantity = Integer.parseInt(String.valueOf(getDetail(lnCtr, "nQuantity")));
            lnUnitPrce = ((Number) getDetail(lnCtr, "nUnitPrce")).doubleValue();
            lnDiscount = ((Number) getDetail(lnCtr, "nDiscount")).doubleValue() / 100;
            lnAddDiscx = ((Number) getDetail(lnCtr, "nAddDiscx")).doubleValue();
            lnDetlTotl = (lnQuantity * (lnUnitPrce - (lnUnitPrce * lnDiscount))) + lnAddDiscx;
            
            lnLaborTotl += lnDetlTotl;
        }
        
        lnRow = getPartsCount();
        for (int lnCtr = 0; lnCtr < lnRow; lnCtr++){
            lnQuantity = Integer.parseInt(String.valueOf(getParts(lnCtr, "nQuantity")));
            lnUnitPrce = ((Number) getParts(lnCtr, "nUnitPrce")).doubleValue();
            lnDiscount = ((Number) getParts(lnCtr, "nDiscount")).doubleValue() / 100;
            lnAddDiscx = ((Number) getParts(lnCtr, "nAddDiscx")).doubleValue();
            lnDetlTotl = (lnQuantity * (lnUnitPrce - (lnUnitPrce * lnDiscount))) + lnAddDiscx;
            
            lnPartsTotl += lnDetlTotl;
        }
        
        lnTranTotal = lnLaborTotl + lnPartsTotl;
        
        p_oMaster.first();
        p_oMaster.updateObject("nLabrTotl", lnLaborTotl);
        p_oMaster.updateObject("nPartTotl", lnPartsTotl);
        p_oMaster.updateObject("nTranTotl", lnTranTotal);
        p_oMaster.updateRow();
        
        if (p_oListener != null) p_oListener.MasterRetreive("nLabrTotl", getMaster("nLabrTotl"));
        if (p_oListener != null) p_oListener.MasterRetreive("nPartTotl", getMaster("nPartTotl"));
        if (p_oListener != null) p_oListener.MasterRetreive("nTranTotl", getMaster("nTranTotl"));
        
        saveToDisk(RecordStatus.ACTIVE, "");
    }
    
    public boolean DeleteTempTransaction(Temp_Transactions foValue) {
        boolean lbSuccess =  CommonUtil.saveTempOrder(p_oNautilus, foValue.getSourceCode(), foValue.getOrderNo(), foValue.getPayload(), "0");
        loadTempTransactions();
        
        p_nEditMode = EditMode.UNKNOWN;
        return lbSuccess;
    }
    
    private void getDetail(int fnRow, String fsFieldNm, Object foValue) throws SQLException, ParseException{       
        JSONObject loJSON;
        JSONParser loParser = new JSONParser();
        
        switch(fsFieldNm){
            case "sLaborCde":
                loJSON = searchLabor("sLaborCde", foValue, true);
                
                if ("success".equals((String) loJSON.get("result"))){
                    loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);
                    
                    //check if the stock id was already exists
                    boolean lbExist = false;
                    
                    for (int lnCtr = 0; lnCtr <= getItemCount() - 1; lnCtr ++){
                        p_oDetail.absolute(lnCtr + 1);
                        if (((String) p_oDetail.getObject("sLaborCde")).equals((String) loJSON.get("sLaborCde"))){
                            fnRow = lnCtr;
                            lbExist = true;
                            break;
                        }
                    }
                    
                    p_oDetail.absolute(fnRow + 1);
                    p_oDetail.updateObject(MiscUtil.getColumnIndex(p_oDetail, "sLaborCde"), (String) loJSON.get("sLaborCde"));
                    p_oDetail.updateObject(MiscUtil.getColumnIndex(p_oDetail, "sLaborNme"), (String) loJSON.get("sDescript"));
                    p_oDetail.updateObject(MiscUtil.getColumnIndex(p_oDetail, "nQuantity"), Integer.parseInt(String.valueOf(p_oDetail.getObject("nQuantity"))) + 1);
                    p_oDetail.updateObject(MiscUtil.getColumnIndex(p_oDetail, "nUnitPrce"), (Number) loJSON.get("nPriceLv3"));
                    p_oDetail.updateRow();      
                    
                    if (!lbExist) addDetail();
                }
        }
    }
    
    private void getParts(int fnRow, String fsFieldNm, Object foValue) throws SQLException, ParseException{       
        JSONObject loJSON;
        JSONParser loParser = new JSONParser();
        
        switch(fsFieldNm){
            case "sStockIDx":
                loJSON = searchParts("a.sStockIDx", foValue, true);
                
                if ("success".equals((String) loJSON.get("result"))){
                    loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);
                    
                    //check if the stock id was already exists
                    boolean lbExist = false;
                    
                    for (int lnCtr = 0; lnCtr <= getPartsCount()- 1; lnCtr ++){
                        p_oPartsx.absolute(lnCtr + 1);
                        if (((String) p_oPartsx.getObject("sStockIDx")).equals((String) loJSON.get("sStockIDx"))){
                            fnRow = lnCtr;
                            lbExist = true;
                            break;
                        }
                    }
                    
                    p_oPartsx.absolute(fnRow + 1);
                    p_oPartsx.updateObject(MiscUtil.getColumnIndex(p_oPartsx, "sStockIDx"), (String) loJSON.get("sStockIDx"));
                    p_oPartsx.updateObject(MiscUtil.getColumnIndex(p_oPartsx, "nUnitPrce"), (Number) loJSON.get("nSelPrce1"));
                    p_oPartsx.updateObject(MiscUtil.getColumnIndex(p_oPartsx, "nQuantity"), Integer.parseInt(String.valueOf(p_oPartsx.getObject(MiscUtil.getColumnIndex(p_oPartsx, "nQuantity")))) + 1);
                    
                    p_oPartsx.updateObject(MiscUtil.getColumnIndex(p_oPartsx, "sBarCodex"), (String) loJSON.get("sBarCodex"));
                    p_oPartsx.updateObject(MiscUtil.getColumnIndex(p_oPartsx, "sDescript"), (String) loJSON.get("sDescript"));
                    p_oPartsx.updateObject(MiscUtil.getColumnIndex(p_oPartsx, "nQtyOnHnd"), Integer.parseInt(String.valueOf(loJSON.get("nQtyOnHnd"))));
                    p_oPartsx.updateObject(MiscUtil.getColumnIndex(p_oPartsx, "sBrandCde"), (String) loJSON.get("sBrandCde"));
                    p_oPartsx.updateObject(MiscUtil.getColumnIndex(p_oPartsx, "sModelCde"), (String) loJSON.get("sModelCde"));
                    p_oPartsx.updateObject(MiscUtil.getColumnIndex(p_oPartsx, "sColorCde"), (String) loJSON.get("sColorCde"));
                    p_oPartsx.updateRow();                    
                    if (!lbExist) addParts();
                }
        }
    }
    
    private void getMaster(String fsFieldNm, Object foValue) throws SQLException, ParseException{       
        JSONObject loJSON;
        JSONParser loParser = new JSONParser();
        
        switch(fsFieldNm){
            case "sClientID":
                loJSON = searchClient("a.sClientID", foValue, true);
                
                if ("success".equals((String) loJSON.get("result"))){
                    loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);
                    
                    p_oMaster.first();
                    p_oMaster.updateObject("sClientID", (String) loJSON.get("sClientID"));
                    p_oMaster.updateObject("xClientNm", (String) loJSON.get("sClientNm"));
                    p_oMaster.updateRow();
                    
                    if (p_oListener != null) p_oListener.MasterRetreive("xClientNm", getMaster("xClientNm"));
                }
                break;
            case "sSerialID":
                loJSON = searchSerial("a.sSerialID", foValue, true);
                
                if ("success".equals((String) loJSON.get("result"))){
                    loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);
                    
                    p_oMaster.first();
                    p_oMaster.updateObject("sSerialID", (String) loJSON.get("sSerialID"));
                    p_oMaster.updateObject("xEngineNo", (String) loJSON.get("sSerial01"));
                    p_oMaster.updateObject("xFrameNox", (String) loJSON.get("sSerial02"));
                    p_oMaster.updateRow();
                    
                    if (p_oListener != null) p_oListener.MasterRetreive("xEngineNo", getMaster("xEngineNo"));
                    if (p_oListener != null) p_oListener.MasterRetreive("xFrameNox", getMaster("xFrameNox"));
                }
                break;
            case "sDealerCd":
                loJSON = searchMCDealer("sDealerCd", foValue, true);
                
                if ("success".equals((String) loJSON.get("result"))){
                    loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);

                    p_oMaster.first();
                    p_oMaster.updateObject("sDealerCd", (String) loJSON.get("sDealerCd"));
                    p_oMaster.updateObject("xDealerNm", (String) loJSON.get("sDescript"));
                    p_oMaster.updateRow();
                    
                    if (p_oListener != null) p_oListener.MasterRetreive("xDealerNm", getMaster("xDealerNm"));
                }
                break;
            case "sMechanic":
                loJSON = searchMechanic("a.sClientID", foValue, true);
                
                if ("success".equals((String) loJSON.get("result"))){
                    loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);
                    
                    p_oMaster.first();
                    p_oMaster.updateObject("sMechanic", (String) loJSON.get("sClientID"));
                    p_oMaster.updateObject("xMechanic", (String) loJSON.get("sClientNm"));
                    p_oMaster.updateRow();
                    
                    if (p_oListener != null) p_oListener.MasterRetreive("xMechanic", getMaster("xMechanic"));
                }
                break;
            case "sSrvcAdvs":
                loJSON = searchAdvisor("a.sClientID", foValue, true);
                
                if ("success".equals((String) loJSON.get("result"))){
                    loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);
                    
                    p_oMaster.first();
                    p_oMaster.updateObject("sSrvcAdvs", (String) loJSON.get("sClientID"));
                    p_oMaster.updateObject("xSrvcAdvs", (String) loJSON.get("sClientNm"));
                    p_oMaster.updateRow();
                    
                    if (p_oListener != null) p_oListener.MasterRetreive("xSrvcAdvs", getMaster("xSrvcAdvs"));
                }
                break;
            case "sTermCode":
                loJSON = searchTerm("sTermCode", foValue, true);
                
                if ("success".equals((String) loJSON.get("result"))){
                    loJSON = (JSONObject) ((JSONArray) loParser.parse((String) loJSON.get("payload"))).get(0);
                    
                    p_oMaster.first();
                    p_oMaster.updateObject(MiscUtil.getColumnIndex(p_oMaster, "sTermCode"), (String) loJSON.get("sTermCode"));
                    p_oMaster.updateObject(MiscUtil.getColumnIndex(p_oMaster, "xTermName"), (String) loJSON.get("sDescript"));
                    p_oMaster.updateRow();
                    
                    if (p_oListener != null) p_oListener.MasterRetreive("xTermName", getMaster("xTermName"));
                }
                break;
        }
    }
}
