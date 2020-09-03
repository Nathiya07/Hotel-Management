package com.fieldforce.dao;

import static org.hamcrest.CoreMatchers.instanceOf;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import net.coobird.thumbnailator.Thumbnails;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.IOUtils;
import org.apache.velocity.app.VelocityEngine;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.primefaces.component.api.UIColumn;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import org.primefaces.model.UploadedFile;
import org.primefaces.model.map.LatLng;
import org.primefaces.model.map.LatLngBounds;
import org.slf4j.Logger;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fieldforce.bean.AssetManagementBean;
import com.fieldforce.bean.AssetManagementReportBean;
import com.fieldforce.bean.CappsurePreferencesBean;
import com.fieldforce.bean.DashboardBean;
import com.fieldforce.bean.ForemanIncentiveBean;
import com.fieldforce.bean.ForemanSupplierInventoryBean;
import com.fieldforce.bean.GpsReportBean;
import com.fieldforce.bean.GreenSheetBean;
import com.fieldforce.bean.IrrigationServicesInvoiceReportBean;
import com.fieldforce.bean.LoginBean;
import com.fieldforce.bean.MaintenanceBean;
import com.fieldforce.bean.ProposalBean;
import com.fieldforce.bean.ProposalMaintenanceStatus;
import com.fieldforce.bean.WorkOrderBean;
import com.fieldforce.bean.WorkorderInvoiceReportBean;
import com.fieldforce.cache.CacheUtil;
import com.fieldforce.cache.CacheUtil.InventoryCacheKey;
import com.fieldforce.cache.UserCacheKey;
import com.fieldforce.common.InventoryClassification;
import com.fieldforce.common.PaymentTypeEnum;
import com.fieldforce.common.ProposalApprovalStatus;
import com.fieldforce.common.UserType;
import com.fieldforce.dto.AccountingDTO;
import com.fieldforce.dto.CappsurePreferencesDTO;
import com.fieldforce.dto.CappsurePreferencesValueDTO;
import com.fieldforce.dto.ClientDTO;
import com.fieldforce.dto.ClientPropertyDTO;
import com.fieldforce.dto.CompanyDTO;
import com.fieldforce.dto.FacesFilterMappingDTO;
import com.fieldforce.dto.ForemanMaintainActivityDTO;
import com.fieldforce.dto.ForemanToggleControlsDTO;
import com.fieldforce.dto.ForemanTogglesMasterCtrlDTO;
import com.fieldforce.dto.PlaceDTO;
import com.fieldforce.dto.PreferencesDTO;
import com.fieldforce.dto.PreferencesValueDTO;
import com.fieldforce.dto.UserDTO;
import com.fieldforce.rest.resources.UserEmailListResource;
import com.fieldforce.setting.AESEncryption;
import com.fieldforce.setting.ConnectionHelper;
import com.fieldforce.setting.DBConnector;
import com.fieldforce.setting.DBQueryLoader;
import com.fieldforce.setting.DBResourseLoader;
import com.fieldforce.setting.MessageLoader;
import com.fieldforce.setting.QuickBooksLoader;
import com.fieldforce.util.CommonUtil;
import com.fieldforce.util.CompositeKey;
import com.fieldforce.util.ForemanUtil;
import com.fieldforce.util.S3Util;
import com.fieldforce.util.UserUtil;
import com.google.gson.Gson;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.itextpdf.text.log.SysoCounter;

/**
 * @author dhanaraj.r
 * 
 */

/**
 * V.No		Modified By			Date			Reason
 * ====		===========			====			==============================================================
 * 1.0		Muthu vignesh. K	02-13-2018		Fixed the issue adding inventory by SuperAdmin Foreman
 * 1.1		Muthu vignesh. K	02-22-2018		Changes made to convert image with loss less quality
 * 1.2		Muthu vignesh. K	03-21-2018		Changes has been done to fetch super admin details in back office report
 * 1.3		Muthu vignesh. K	03-23-2018		Changes done to set the parameter sorting order
 * 1.4 		Muthu vignesh. K	06-27-2018		Changes made to get the polygon coordinates
 * 1.5 		Muthu vignesh. K	07-25-2018		Changes made to update the expected pings
 * 1.6 		Muthu vignesh. K	08-06-2018		Changes made for module access control and faces filter mapping
 * 1.7		Muthu vignesh. K	10-08-2018		Changes made for User master table control
 * 1.8		Muthu vignesh. K	10-12-2018		Changes done to get the company details for admin role
 * 1.9		Muthu vignesh. K	10-22-2018		Changes done for user multiple company login
 * 1.10		Muthu vignesh. K	11-25-2018		Changes done for maintenance report module
 * 1.11		Muthu vignesh. K	12-04-2018		Changes made for GPS report filter by proeprties
 * 1.12		Muthu vignesh. K	12-26-2018		Changes done for multiple filters
 * 1.13		Muthu vignesh. K	03-21-2019		Changes done for getting foreman toggle master values by position
 * 1.14		Muthu vignesh. K	04-10-2019		Adding new module access for dashboard
 * 1.15		Ramanan.G			09-06-2019		Changes made to add upload speed, download speed parameters in maintainForemanActivityLog
 * 1.16		Ramanan.G			11-19-2019		Changes made to add AWS URL and image sizes 
 * 1.17		Ramanan.G			11-27-2019		Changes made to add AWS URL and get image width/height and process duration
 * 1.18     Nathiya.M			12-18-2019		Added new method for fetching monthly payment details from ff_company_management
 * 1.19		Nathiya.M			03-13-2020		Modified code for implementing mirage boundary changes.
 * 1.20		Nathiya.M			03-18-2020		Modified code for implementing customized beacon changes.
 * 1.21		Kalaivani. S		04-15-2020		Changes made for get the asset name details
 * 1.22		Kalaivani. S		05-08-2020		Added Background Service Location changes
 * 1.23     Prasanth D          05-14-2020    Changes made for Grace period
 * 1.24		Kalaivani. S		05-27-2020		Changes made for remove the empty string in asset filter
 * 1.25		Nathiya.M			05-28-2020		Modified code for implementing Captcha Algorithm changes.
*/

public class CommonDAO extends AbstractDAO {

	private static final long serialVersionUID = 1L;

	public static final String dateTimeFormat = "MM/dd/yyyy hh:mm a";
	public static final String yearFormat = "yy";
	public static final String dateFormatEamil = "MM/dd/yyyy";

	public static final String timeFormatEamil = "HH:mm";

	public static final String timeFormat12Hours = "hh:mm a";

	public static final String dateFormatSQL = "yyyy-MM-dd";
	public static final String dateTimeFormatSQL = "yyyy-MM-dd kk:mm:ss";

	public static final String dateTimeFormatISO = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	private static Integer THUMBNAIL_IMAGE_WIDTH = 200;
	private static Integer THUMBNAIL_IMAGE_HEIGHT = 200;

	public static final String serverTimezone = "UTC";

	private static final String exception = "Select Exception :";
	private static final String SQLException = "Select SQLException..";
	private static final String WO = "WO";

	private static final String COMPANY_S3_URL = MessageLoader.getInstance()
			.getMessageStatement("MSG_S3_UPLOAD_COMPANY_LOGO_FOLDER");

	private static final String S3_BUCKET_HOME = MessageLoader.getInstance()
			.getMessageStatement("MSG_S3_BUCKET_HOME_URL");
	
	private static final String URL_TO_HIDE = "https://elasticbeanstalk-us-west-2-326764779765.s3.amazonaws.com/";
	private static final String MOBILE_AWS_BUKCET = "elasticbeanstalk-us-west-2-326764779765";
	private static ThreadPoolExecutor executorService = new ThreadPoolExecutor(1, 10, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());


	private static Logger logger = DBResourseLoader.getInstance().getLogger(
			CommonDAO.class);

	public CommonDAO() {
	}
	
	//v1.6 Starts
	private String moduleName = null;
	private Object beanObject = null;
	
	
	public CommonDAO(Object beanObject, String moduleName) {
		this.beanObject = beanObject;
		this.moduleName = moduleName;
	}
	//v1.6 Ends

	/* validating values for fields */
	public boolean isStringEmpty(String value) {
		if (value == null || value.trim().equals("")) {
			return true;
		}
		return false;
	}

	public void qbSource(String qbSource) {
		try {
			CommonDAO commonDAO = new CommonDAO();
			commonDAO.getSetSession("qbSource", qbSource, "");
			// logger.debug("QB : " + qbSource);
		} catch (Exception e) {
			logger.error("====", e);
		}
	}

	public Map<String, Integer> readingCountOFQBValues(String companyID,
			String userID, String accesstoken, String accessstokensecret,
			String realmID, String module) {

		double totalCount = 0;
		Map<String, Integer> map = new HashMap<String, Integer>();
		HttpURLConnection urlConnection = null;
		QuickBooksDAO quickBooksDAO = new QuickBooksDAO();
		List<AccountingDTO> accountingDTOList = new ArrayList<AccountingDTO>();
		try {

			String jsonData = "";
			URL url = null;
			int countTimes = 0;
			PreferencesDAO preferencesDAO = new PreferencesDAO();
			AccountingDTO accountingDTO = new AccountingDTO(0);

			if (preferencesDAO.isQBAccountingForCompany(Integer
					.valueOf(companyID))) {

				accountingDTO = preferencesDAO.getAccountingByComId(Integer
						.valueOf(companyID));
			}

			try {
				if (accesstoken != null && accessstokensecret != null
						&& realmID != null && accesstoken.trim().length() > 0
						&& accessstokensecret.trim().length() > 0
						&& realmID.trim().length() > 0) {
					if (accountingDTO.getRealmId().length() > 0) {
						accountingDTO.setAccessToken(accesstoken);
						accountingDTO.setAccessTokenSecret(accessstokensecret);
						accountingDTO.setRealmId(realmID);
						preferencesDAO.updateAccountingVendorForCompany(
								accountingDTO, Integer.valueOf(companyID));

					} else {
						accountingDTO.setAccessToken(accesstoken);
						accountingDTO.setAccessTokenSecret(accessstokensecret);
						accountingDTO.setRealmId(realmID);
						preferencesDAO.addAccountingVendorForCompany(
								accountingDTO, Integer.valueOf(companyID));
					}
					/**
					 * Updating token for the company which has this specific
					 * realm ID
					 */
					accountingDTOList = preferencesDAO
							.getCompanyIdsForRealmID(accountingDTO.getRealmId());
					if (accountingDTOList.size() > 0) {
						for (AccountingDTO accountingDTOs : accountingDTOList) {
							if (!(companyID.equals(accountingDTOs
									.getCompanyId()))) {
								preferencesDAO
										.updateAccountingVendorForCompany(
												accountingDTO,
												Integer.valueOf(accountingDTOs
														.getCompanyId()));
							}
						}
					}

				} else {

					accesstoken = accountingDTO.getAccessToken();
					accessstokensecret = accountingDTO.getAccessTokenSecret();
					realmID = accountingDTO.getRealmId();
				}

			} catch (Exception e) {
				logger.error("Exception in storing access keys :", e);
			}

			/**
			 * Using keys connection established and retrieving the customer
			 * data
			 */
			OAuthConsumer ouathconsumer = new DefaultOAuthConsumer(
					QuickBooksLoader.getInstance().getQuickBooksKey(
							"CONSUMER_KEY"), QuickBooksLoader.getInstance()
							.getQuickBooksKey("CONSUMER_SECRET_KEY"));

			ouathconsumer.setTokenWithSecret(accesstoken, accessstokensecret);

			try {
				if (module != null && module.length() > 0
						&& module.equalsIgnoreCase("Property")) {
					url = new URL(QuickBooksLoader.getInstance()
							.getQuickBooksKey("QBO_URL")
							+ realmID
							+ "/query?query=Select+count(*)+from+Customer");

				} else if (module != null && module.length() > 0
						&& module.equalsIgnoreCase("Inventory")) {
					url = new URL(QuickBooksLoader.getInstance()
							.getQuickBooksKey("QBO_URL")
							+ realmID
							+ "/query?query=Select+count(*)+from+Item");

				}

				urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestMethod("GET");
				urlConnection.setUseCaches(false);
				urlConnection.setDoInput(true);
				urlConnection.setDoOutput(true);
				urlConnection.setRequestProperty("Connection", "Keep-Alive");
				urlConnection.setRequestProperty("Content-Type",
						"application/json");
				urlConnection.setRequestProperty("Accept", "application/json");

				ouathconsumer.sign(urlConnection);

				urlConnection.connect();

				if (urlConnection != null) {

					String line;
					BufferedReader rd = new BufferedReader(
							new InputStreamReader(
									urlConnection.getInputStream()));

					while ((line = rd.readLine()) != null) {
						jsonData += line + "\n";
					}
					rd.close();
					JSONObject jsonDataObj = new JSONObject(jsonData);
					JSONObject jsonQueryResponse = jsonDataObj
							.getJSONObject("QueryResponse");

					try {
						totalCount = Integer.valueOf(jsonQueryResponse.get(
								"totalCount").toString());

						countTimes = (int) Math.ceil(totalCount / 200.0);

						int proposalGroupID = 0, irrigationGroupID = 0, proposalCategoryID = 0, irrigationCategoryID = 0;
						if (module.equalsIgnoreCase("Inventory")) {
							proposalGroupID = quickBooksDAO
									.addDefaultProposalGroup(companyID, userID);
							irrigationGroupID = quickBooksDAO
									.addDefaultIrrigationGroup(companyID,
											userID);

							proposalCategoryID = quickBooksDAO
									.addDefaultProposalCategory(companyID,
											userID, proposalGroupID);
							irrigationCategoryID = quickBooksDAO
									.addDefaultIrrigationCategory(companyID,
											userID, irrigationGroupID);

						}
						map.put("totalCount", (int) totalCount);
						map.put("countTimes", (int) countTimes);
						map.put("proposalGroupID", proposalGroupID);
						map.put("irrigationGroupID", irrigationGroupID);
						map.put("proposalCategoryID", proposalCategoryID);
						map.put("irrigationCategoryID", irrigationCategoryID);
					} catch (Exception e) {
						logger.error("Total count splitting error", e);
					}
				}

			} catch (Exception e) {
				logger.error("Url connection not connected", e);
			}
		} catch (Exception e) {
			logger.error("Connection to Quickbooks errors", e);
		}
		return map;

	}

	public Boolean isValidQBAccount(String accesstoken,
			String accessstokensecret, String realmID) {

		double totalCount = 0;
		HttpURLConnection urlConnection = null;
		try {

			String jsonData = "";
			URL url = null;
			/**
			 * Using keys connection established and retrieving the customer
			 * data
			 */
			OAuthConsumer ouathconsumer = new DefaultOAuthConsumer(
					QuickBooksLoader.getInstance().getQuickBooksKey(
							"CONSUMER_KEY"), QuickBooksLoader.getInstance()
							.getQuickBooksKey("CONSUMER_SECRET_KEY"));

			ouathconsumer.setTokenWithSecret(accesstoken, accessstokensecret);

			url = new URL(QuickBooksLoader.getInstance().getQuickBooksKey(
					"QBO_URL")
					+ realmID + "/query?query=Select+count(*)+from+Customer");

			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setUseCaches(false);
			urlConnection.setDoInput(true);
			urlConnection.setDoOutput(true);
			urlConnection.setRequestProperty("Connection", "Keep-Alive");
			urlConnection
					.setRequestProperty("Content-Type", "application/json");
			urlConnection.setRequestProperty("Accept", "application/json");

			ouathconsumer.sign(urlConnection);

			urlConnection.connect();

			if (urlConnection != null) {
				logger.error(" ********* Connected QB Validation **************");
				String line;
				BufferedReader rd = new BufferedReader(new InputStreamReader(
						urlConnection.getInputStream()));

				while ((line = rd.readLine()) != null) {
					jsonData += line + "\n";
				}
				rd.close();
				JSONObject jsonDataObj = new JSONObject(jsonData);
				JSONObject jsonQueryResponse = jsonDataObj
						.getJSONObject("QueryResponse");

				try {
					totalCount = Integer.valueOf(jsonQueryResponse.get(
							"totalCount").toString());

				} catch (Exception e) {
					logger.error("Total count splitting error", e);
				}
				if (totalCount > 0) {
					return true;
				}
			}

		} catch (Exception e) {
			logger.error("Connection to Quickbooks errors : ", e);
		}
		return false;

	}

	// Load Foreman SelectItem
	// public List<SelectItem> loadForemanList(int companyId) {
	// DBConnector db2Connector = DBConnector.getInstance();
	// boolean retry;
	// int numOfretry = 0;
	// Connection con = null;
	// PreparedStatement prepStmnt = null;
	// ResultSet resInfo = null;
	// List<SelectItem> foremanList = new ArrayList<SelectItem>();
	// do {
	// retry = false;
	// try {
	// con = db2Connector.getConnection(true);
	// String selectStatement =
	// "Select usrId, usrName from ff_user_management where usrComId=? and
	// usrRowStatus is null and usrRole='Foreman' order by usrName";
	// prepStmnt = con.prepareStatement(selectStatement);
	// prepStmnt.setString(1, "" + companyId);
	// resInfo = prepStmnt.executeQuery();
	// while (resInfo.next()) {
	// foremanList.add(new SelectItem(
	// "" + resInfo.getInt("usrId"), getString(resInfo,
	// "usrName", "")));
	// }
	// } catch (SQLException scon) {
	// logger.error("Select SQLException.." + scon + " " + numOfretry);
	// if (numOfretry < 2) {
	// numOfretry++;
	// try {
	// close(con);
	// } catch (Exception e) {
	// }
	// con = db2Connector.getConnection(true);
	// retry = true;
	// } else {
	// retry = false;
	// logger.error("Select Exception :" + scon.getMessage());
	// }
	// } finally {
	// close(resInfo);
	// close(prepStmnt);
	// close(con);
	// }
	// } while (retry);
	// return foremanList;
	// }

	/**
	 * Get foreman list based on company id and foreman row status.
	 * foremanActiveRowStatus is false - Get active foreman list
	 * only[usrRowStatus is null]. foremanActiveRowStatus is true - Get all
	 * foreman list[usrRowStatus is null && usrRowStatus is deleted].
	 * 
	 * @param companyId
	 * @param foremanActiveRowStatus
	 * @return foremanList
	 */
	public List<SelectItem> loadForemanActiveInActiveList(int companyId,
			boolean foremanActiveRowStatus) {
		return loadForemanActiveInActiveList(companyId, foremanActiveRowStatus, null);
	}
	
	/**
	 * Get foreman list based on company id and foreman row status.
	 * foremanActiveRowStatus is false - Get active foreman list
	 * only[usrRowStatus is null]. foremanActiveRowStatus is true - Get all
	 * foreman list[usrRowStatus is null && usrRowStatus is deleted].
	 * 
	 * @param companyId
	 * @param foremanActiveRowStatus
	 * @return foremanList
	 */
	public List<SelectItem> loadForemanActiveInActiveList(int companyId,
			boolean foremanActiveRowStatus, LoginBean loginBean) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> foremanList = new ArrayList<SelectItem>();
		
		//v1.6 Starts
		boolean isNeedToBeFiltered = false;
		//v1.6 Ends
		
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "";
				
				//v1.6 Starts
				/*if(beanObject!=null) {
					
					if(this.beanObject instanceof ForemanIncentiveBean) {
						ForemanIncentiveBean foremanIncentiveBean = (ForemanIncentiveBean) beanObject;
						loginBean = foremanIncentiveBean.getLoginBean();
					}
					
					if(this.beanObject instanceof ForemanSupplierInventoryBean) {
						ForemanSupplierInventoryBean foremanSupplierInventoryBean = (ForemanSupplierInventoryBean) beanObject;
						loginBean = foremanSupplierInventoryBean.getLoginBean();
					}
					
					//v1.11 Starts
					if(this.beanObject instanceof GpsReportBean) {
						GpsReportBean gpsReportBean = (GpsReportBean) beanObject;
						loginBean = gpsReportBean.getLoginBean();
					}
					//v1.1 Ends
					
					//v1.12 Starts
					if(this.beanObject instanceof ProposalBean) {
						ProposalBean proposalBean = (ProposalBean) beanObject;
						loginBean = proposalBean.getLoginBean();
					}
					
					if(this.beanObject instanceof MaintenanceBean) {
						MaintenanceBean maintenanceBean = (MaintenanceBean) beanObject;
						loginBean = maintenanceBean.getLoginBean();
					}
					//v1.12 Ends
					
					//v1.14 Starts
					if(this.beanObject instanceof DashboardBean) {
						DashboardBean dashboardBean = (DashboardBean) beanObject;
						loginBean = dashboardBean.getLoginBean();
					}
					//v1.14 Ends
					
					if(loginBean!=null) {
						ForemanUtil foremanUtil = new ForemanUtil();
						
						isNeedToBeFiltered = foremanUtil.getForemanFilterByAssgdProperties(loginBean.getUserId(),
								loginBean.getCacheUserRole(), moduleName);
					}
					
				}*/
				
				if(loginBean!=null) {
					ForemanUtil foremanUtil = new ForemanUtil();
					
					isNeedToBeFiltered = foremanUtil.getForemanFilterByAssgdProperties(loginBean.getUserId(),
							loginBean.getCacheUserRole(), moduleName);
				}

				if (foremanActiveRowStatus) {
					//v1.2 Starts
					//selectStatement = "Select usrId, usrName from ff_user_management where usrComId=? and usrRole='Foreman' order by usrName";
					selectStatement = "Select usrId, usrName from ff_user_management where usrComId=? and (usrRole='Foreman' or usrRole='Super Admin') order by usrName";
					//v1.2 Ends
				} else {
					//v1.2 Starts
					//selectStatement = "Select usrId, usrName from ff_user_management where usrComId=? and usrRowStatus is null and usrRole='Foreman' order by usrName";
					//v1.6 Starts
					//selectStatement = "Select usrId, usrName from ff_user_management where usrComId=? and usrRowStatus is null and (usrRole='Foreman' or usrRole='Super Admin') order by usrName";
					selectStatement = isNeedToBeFiltered ? 
								" Select usrId, usrName from "
								+ " ff_user_management "
								+ " where usrComId=? "
								+ " and usrRowStatus is null "
								+ " and usrId in (SELECT cfUsrId FROM ff_client_foreman_management WHERE cfCmId in "  
								+ " ( SELECT cfCmId FROM ff_client_foreman_management WHERE cfUsrId = ?)) "
								+ " and (usrRole='Foreman' or usrRole='Super Admin') order by usrName ":
								"Select usrId, usrName from ff_user_management where usrComId=? and usrRowStatus is null and (usrRole='Foreman' or usrRole='Super Admin') order by usrName";
					//v1.6 Ends
					//v1.2 Ends
				}
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				
				logger.info("Is need to be filtered falg for foreman is ->"+isNeedToBeFiltered);
				//v1.6 Starts
				if(isNeedToBeFiltered) {
					prepStmnt.setInt(2, loginBean.getUserId());
				}
				
				//v1.6 Ends
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					foremanList.add(new SelectItem(
							"" + resInfo.getInt("usrId"), getString(resInfo,
									"usrName", "")));
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return foremanList;
	}

	public List<SelectItem> loadCompanyList() {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> foremanList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select comId, comName from ff_company_management where comRowStatus is null ";
				prepStmnt = con.prepareStatement(selectStatement);

				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					foremanList.add(new SelectItem(
							"" + resInfo.getInt("comId"), getString(resInfo,
									"comName", "")));
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return foremanList;
	}

	public List<SelectItem> loadAccountPlanList() {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> accountPlanList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select * from ff_subscription_plan where spRowStatus is null";
				prepStmnt = con.prepareStatement(selectStatement);

				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					accountPlanList.add(new SelectItem(""
							+ resInfo.getInt("spId"), getString(resInfo,
							"spName", "")));
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return accountPlanList;
	}

	// Load Inventory Work order SelectItem based on Type(Proposal,Maintenance
	// and Irrigation)
	public List<SelectItem> loadInventoryWorkorderListType(int companyId,
			String Itemtype, String creator) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> clientList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				/*
				 * Altered SQL query to select both null or 'Work Order' value
				 * for ic/ig/ii RowSatus column
				 */
				String selectStatement = "Select iiId,igName,icName,iiName,iiLongDescription,iiShowToClient,"
						+ " igClientDescription,icClientDescription,icClientDescription,iiClientDescription,iiLongDescription "
						+ "from ff_inventory_item inner join ff_inventory_category on iiIcId=icId "
						+ " inner join ff_inventory_group on icIgId=igId "
						+ "where icComId=? and igType = ? and NULLIF(igRowStatus,'Work Order') is NULL  and NULLIF(icRowStatus,'Work Order') is NULL "
						+ " and NULLIF(iiRowStatus,'Work Order') is NULL"
						+ " and icVendorStatus is null and iiVendorStatus is null ";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				prepStmnt.setString(2, Itemtype);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					String icName = getString(resInfo, "icName", "");
					String selectItemValue = "";
					if (icName
							.equals(CommonVariables.WORK_ORDER_INVENTORY_CATEGORY_NAME)
							|| icName
									.equals(CommonVariables.INVENTORY_DEFAULT_CATEGORY_NAME)) {
						selectItemValue = getString(resInfo, "iiName", "")
								+ " - "
								+ getString(resInfo, "iiLongDescription", "");
					} else {
						selectItemValue = getString(resInfo, "icName", "")
								+ " - "
								+ getString(resInfo, "iiLongDescription", "");
					}
					if (creator.equals("")
							|| creator.equalsIgnoreCase("Foreman")) {
						clientList.add(new SelectItem(resInfo.getInt("iiId"),
								selectItemValue));
					} else {
						if (resInfo.getInt("iiShowToClient") == 1) {
							clientList.add(new SelectItem(resInfo
									.getInt("iiId"), getString(resInfo,
									"icClientDescription", "")
									+ " - "
									+ getString(resInfo, "iiLongDescription",
											"")));
						}
					}
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return clientList;
	}

	public List<UploadedFile> loadImageWorkorderList(int LineId,
			String Imagetype) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<UploadedFile> imageList = new ArrayList<UploadedFile>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select * from ff_workorder_image where woiWolId = ? and woiType = ?";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, LineId);
				prepStmnt.setString(2, Imagetype);
				resInfo = prepStmnt.executeQuery();
				imageList.add((UploadedFile) resInfo.getBlob("woiImage"));
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return imageList;

	}

	// get foreman id by work order id
	public int getForemanIdByWorkorderId(Integer woId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;

		int foremanId = 0;
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "Select usrId from ff_workorder_header join ff_user_management on wohForemanId=usrId where wohId = ? ";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, woId);
				resInfo = prepStmnt.executeQuery();

				if (resInfo.next()) {
					foremanId = getInt(resInfo, "usrId", 0);
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return foremanId;
	}

	public int getForemanIdByMaintenanceId(Integer maintenanceId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;

		int foremanId = 0;
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "Select * from ff_maintenance_header "
						+ " join ff_user_management on mhForemanId=usrId where mhID = ? ";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, maintenanceId);
				resInfo = prepStmnt.executeQuery();

				if (resInfo.next()) {
					foremanId = resInfo.getInt("usrId");
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return foremanId;
	}

	// Load group SelectItem
	public List<SelectItem> loadGroupList(int companyId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> groupList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select igId,igName from ff_inventory_group where igComId=? and igRowStatus is null order by igName";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setString(1, "" + companyId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					// if (!(getString(resInfo, "igName", "")
					// .equalsIgnoreCase(CommonVariables.QB_DEFAULT_PROPOSAL_GROUP))
					// && !(getString(resInfo, "igName", "")
					// .equalsIgnoreCase(CommonVariables.QB_DEFAULT_IRRIGATION_GROUP)))
					// {
					groupList.add(new SelectItem("" + resInfo.getInt("igId"),
							getString(resInfo, "igName", "")));
					// }
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return groupList;
	}

	public List<SelectItem> loadGroupListByInventoryType(int companyId,
			String inventoryType) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> groupList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select * from ff_inventory_group where igComId=? and igType=? and igRowStatus is null order by igName";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setString(1, "" + companyId);
				prepStmnt.setString(2, inventoryType);

				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					// if (!(getString(resInfo, "igName", "")
					// .equalsIgnoreCase(CommonVariables.QB_DEFAULT_PROPOSAL_GROUP))
					// && !(getString(resInfo, "igName", "")
					// .equalsIgnoreCase(CommonVariables.QB_DEFAULT_IRRIGATION_GROUP)))
					// {
					groupList.add(new SelectItem("" + resInfo.getInt("igId"),
							getString(resInfo, "igName", "")));
					// }

				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return groupList;
	}

	public String getPreferenceValueByPrefixCompanyId(Integer companyId,
			String preferencePrefix) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;

		String preferenceValue = "0";
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "select plValue from ff_preference_header left join ff_preference_line on phId=plPhId "
						+ "where plComId=?  and phPrefix=? ";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				prepStmnt.setString(2, preferencePrefix);

				resInfo = prepStmnt.executeQuery();

				while (resInfo.next()) {
					preferenceValue = getString(resInfo, "plValue", "0");

				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return preferenceValue;
	}

	// getForemanIdByGreenSheetId
	public int getForemanIdByGreenSheetId(Integer greensheetId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;

		int foremanId = 0;
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "Select * from ff_green_sheet_header join ff_user_management on gsForemanId=usrId where gsId = ?";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, greensheetId);
				resInfo = prepStmnt.executeQuery();

				if (resInfo.next()) {
					foremanId = resInfo.getInt("usrId");
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return foremanId;
	}

	// get foreman id by proposal id
	public int getForemanIdByProposalId(Integer propId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;

		int foremanId = 0;
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "Select usrId from ff_proposal_header join ff_user_management on proForemanId=usrId where proId = ? ";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, propId);
				resInfo = prepStmnt.executeQuery();

				if (resInfo.next()) {
					foremanId = getInt(resInfo, "usrId", 0);
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return foremanId;
	}

	// load proposal foreman name
	public List<SelectItem> loadForemanListByClientId(Integer clientId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		int i = 0;
		List<SelectItem> foremanList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select usrId, usrName from ff_user_management left join ff_client_foreman_management on usrId=cfUsrId where cfCmId =? and usrRowStatus is null";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, clientId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					foremanList.add(new SelectItem(
							"" + resInfo.getInt("usrId"), getString(resInfo,
									"usrName", "")));
					i++;
				}
				if (i == 0) {
					foremanList.add(new SelectItem(0, "No Foreman"));
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return foremanList;
	}

	// load client list by property

	public List<SelectItem> loadClientListByPropertyId(Integer propertyId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		int i = 0;
		List<SelectItem> clientList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "SELECT clientId, clientName FROM ff_client_management_line left join ff_client "
						+ "on clClientId=clientId left join ff_client_management "
						+ "on  clCmId=CmId where clientRowStatus is null and cmRowStatus is null and cmId=?";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, propertyId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					clientList.add(new SelectItem(""
							+ resInfo.getInt("clientId"), getString(resInfo,
							"clientName", "")));
					i++;
				}
				if (i == 0) {
					clientList.add(new SelectItem(0, "No Client"));
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return clientList;
	}

	// Find Foreman Name
	public String getForemanNameById(Integer foremanId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		String foremanName = "No Foreman";
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select * from ff_user_management where usrRole='Foreman' and usrId=?";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, foremanId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					foremanName = getString(resInfo, "usrName", "");
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return foremanName;
	}

	// public String getClientForemanNameByClientId(Integer clientId) {
	// DBConnector db2Connector = DBConnector.getInstance();
	// boolean retry;
	// int numOfretry = 0;
	// Connection con = null;
	// PreparedStatement prepStmnt = null;
	// ResultSet resInfo = null;
	// String foremanName = "No Foreman";
	// do {
	// retry = false;
	// try {
	// con = db2Connector.getConnection(true);
	// String selectStatement =
	// "Select usrName from ff_client_foreman_management join " +
	// "ff_user_management on cfUsrId=usrId where usrRole='Foreman' and
	// cfCmId=?";
	// prepStmnt = con.prepareStatement(selectStatement);
	// prepStmnt.setInt(1, clientId);
	// resInfo = prepStmnt.executeQuery();
	// int i = 0;
	// while (resInfo.next()) {
	// if (i == 0) {
	// foremanName = resInfo.getString("usrName");
	// } else {
	// foremanName = foremanName + ","
	// + resInfo.getString("usrName");
	// }
	// i++;
	// }
	// } catch (SQLException scon) {
	// logger.error("Select SQLException.." + scon + " " + numOfretry);
	// if (numOfretry < 2) {
	// numOfretry++;
	// try {
	// close(con);
	// } catch (Exception e) {
	// }
	// con = db2Connector.getConnection(true);
	// retry = true;
	// } else {
	// retry = false;
	// logger.error("Select Exception :" + scon.getMessage());
	// }
	// } finally {
	// close(resInfo);
	// close(prepStmnt);
	// close(con);
	// }
	// } while (retry);
	// return foremanName;
	// }

	// daks---ForeManDTO
	// public ForemanDTO getForemanDTOById(Integer clientId) {
	// DBConnector db2Connector = DBConnector.getInstance();
	// boolean retry;
	// int numOfretry = 0;
	// Connection con = null;
	// PreparedStatement prepStmnt = null;
	// ResultSet resInfo = null;
	// String foremanName = "No Foreman";
	// ForemanDTO foremanDTO = new ForemanDTO(1);
	// ForemanDAO foremanDAO = new ForemanDAO();
	// do {
	// retry = false;
	// try {
	// con = db2Connector.getConnection(true);
	// String selectStatement =
	// "Select * from ff_client_foreman_management join ff_user_management" +
	// " on cfUsrId=usrId where usrRole='Foreman' and cfCmId=?";
	// prepStmnt = con.prepareStatement(selectStatement);
	// prepStmnt.setInt(1, clientId);
	// resInfo = prepStmnt.executeQuery();
	// int i = 0;
	// while (resInfo.next()) {
	// Boolean subClient = false;
	// try {
	// subClient = resInfo.getInt("usrSubmitToClient") == 1 ? true
	// : false;
	// } catch (Exception e) {
	// }
	//
	// // CompanyDAO companyDAO = new CompanyDAO();
	// // CompanyDTO companyDTO = companyDAO
	// // .getCompanyDetailsById(resInfo
	// // .getString("usrComId"));
	//
	// foremanDTO = new ForemanDTO(i, resInfo.getInt("usrId"),
	// getString(resInfo, "usrName", ""), getString(
	// resInfo, "usrEmail", ""), getString(
	// resInfo, "usrPhoneNumber", ""), getString(
	// resInfo, "usrAddress", ""), getString(
	// resInfo, "usrDeviceId", ""), getString(
	// resInfo, "usrCrewSize", ""), getString(
	// resInfo, "usrCrewNumber", ""), subClient,
	// foremanDAO.getForemanPropertyName(resInfo
	// .getInt("usrId")),
	// resInfo.getInt("usrComId"));
	//
	// i++;
	// }
	// } catch (SQLException scon) {
	// logger.error("Select SQLException.." + scon + " " + numOfretry);
	// if (numOfretry < 2) {
	// numOfretry++;
	// try {
	// close(con);
	// } catch (Exception e) {
	// }
	// con = db2Connector.getConnection(true);
	// retry = true;
	// } else {
	// retry = false;
	// logger.error("Select Exception :" + scon.getMessage());
	// }
	// } finally {
	// close(resInfo);
	// close(prepStmnt);
	// close(con);
	// }
	// } while (retry);
	// return foremanDTO;
	// }

	// Load Client SelectItem
	
	public List<SelectItem> loadPropertyList(int companyId) {
		return loadPropertyList(companyId, null);
	}
	
	public List<SelectItem> loadPropertyList(int companyId, LoginBean loginBean) {
		
		//v1.6 Starts
		boolean isNeedToBeFiltered = false;
		//v1.6 Ends
		
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> clientList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				
				//v1.6 Starts
				/*if(beanObject!=null) {
					
					if(this.beanObject instanceof WorkOrderBean) {
						WorkOrderBean workOrderBean = (WorkOrderBean) beanObject;
						loginBean = workOrderBean.getLoginBean();
					}
					
					if(this.beanObject instanceof AssetManagementBean) {
						AssetManagementBean assetManagementBean = (AssetManagementBean) beanObject;
						loginBean = assetManagementBean.getLoginBean();
					}
					
					if(this.beanObject instanceof MaintenanceBean) {
						MaintenanceBean maintenanceBean = (MaintenanceBean) beanObject;
						loginBean = maintenanceBean.getLoginBean();
					}
					
					if(this.beanObject instanceof GreenSheetBean) {
						GreenSheetBean greenSheetBean = (GreenSheetBean) beanObject;
						loginBean = greenSheetBean.getLoginBean();
					}
					
					if(this.beanObject instanceof ProposalBean) {
						ProposalBean proposalBean = (ProposalBean) beanObject;
						loginBean = proposalBean.getLoginBean();
					}
					
					if(this.beanObject instanceof IrrigationServicesInvoiceReportBean) {
						IrrigationServicesInvoiceReportBean irrigationServicesInvoiceReportBean = (IrrigationServicesInvoiceReportBean) beanObject;
						loginBean = irrigationServicesInvoiceReportBean.getLoginBean();
					}
					
					if(this.beanObject instanceof AssetManagementReportBean) {
						AssetManagementReportBean assetManagementReportBean = (AssetManagementReportBean) beanObject;
						loginBean = assetManagementReportBean.getLoginBean();
					}
					
					if(this.beanObject instanceof WorkorderInvoiceReportBean) {
						WorkorderInvoiceReportBean workorderInvoiceReportBean = (WorkorderInvoiceReportBean) beanObject;
						loginBean = workorderInvoiceReportBean.getLoginBean();
					}
					
					//v1.11 Starts
					if(this.beanObject instanceof GpsReportBean) {
						GpsReportBean gpsReportBean = (GpsReportBean) beanObject;
						loginBean = gpsReportBean.getLoginBean();
					}
					//v1.1 Ends
					
					//v1.14 Starts
					if(this.beanObject instanceof DashboardBean) {
						DashboardBean dashboardBean = (DashboardBean) beanObject;
						loginBean = dashboardBean.getLoginBean();
					}
					
					if(this.beanObject instanceof ForemanIncentiveBean) {
						ForemanIncentiveBean foremanIncentiveBean = (ForemanIncentiveBean) beanObject;
						loginBean = foremanIncentiveBean.getLoginBean();
					}
					//v1.14 Ends
					
					if(loginBean!=null) {
						ForemanUtil foremanUtil = new ForemanUtil();
						isNeedToBeFiltered = foremanUtil.getForemanFilterByAssgdProperties(loginBean.getUserId(),
								loginBean.getCacheUserRole(), moduleName);
					}
				}*/
				//v1.6 Ends
				
				if(loginBean!=null) {
					ForemanUtil foremanUtil = new ForemanUtil();
					isNeedToBeFiltered = foremanUtil.getForemanFilterByAssgdProperties(loginBean.getUserId(),
							loginBean.getCacheUserRole(), moduleName);
				}
				
				con = db2Connector.getConnection(true);
				String selectStatement = "Select cmId, cmPropertyName from ff_client_management "
						+ " where cmComId=? and cmRowStatus is null and (cmVendorStatus='' or cmVendorStatus is null) order by cmPropertyName";
				//v1.6 Starts
				if(isNeedToBeFiltered && loginBean!=null) {
					if (CommonVariables.CLIENT.contentEquals(loginBean.getCacheUserRole())) {
						selectStatement = DBQueryLoader.getInstance()
								.getQueryStatement("LOAD_PROPERTIES_BY_CLIENT_FILTER");
					}
					else {
						selectStatement = DBQueryLoader.getInstance()
								.getQueryStatement("LOAD_PROPERTIES_BY_FILTER");
					}
				}
				//v1.6 Ends
				logger.info("Select statement for loading properties is ->"+selectStatement);
				logger.info("isNeedToBeFiltered value for loading properties is ->"+isNeedToBeFiltered);

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				//v1.6 Starts
				if(isNeedToBeFiltered && loginBean!=null) {
					prepStmnt.setInt(2, loginBean.getUserId());
				}
				//v1.6 Ends
				
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					clientList.add(new SelectItem("" + resInfo.getInt("cmId"),
							getString(resInfo, "cmPropertyName", "")));
				}
				logger.info("clientList size is ->"+clientList.size());
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return clientList;
	}

	// Load Client's Company SelectItem
	public List<SelectItem> loadCompanyListByClientId(Integer clientId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> companyList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select ff_client_company.*, comId, comName, comPhoneNumber, comEmail, comPropertyCount, comBillingAddress, "
						+ " comBillingCountry, comBillingState, comBillingCity, comBillingZipCode, comRowStatus, comBillingDate, "
						+ " comBillingAmount, comRecurringBill, isGracePeriod, gracePeriodDate, comActiveStatus,ifnull(comUpdatedTime,comCreatedTime) as comUpdatedCreatedTime from ff_client_company "
						+ " inner join ff_company_management on ccComId=comId "
						+ " where comRowStatus is null and comActiveStatus=1 and ccClientId=?";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, clientId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					companyList.add(new SelectItem(
							"" + resInfo.getInt("comId"), getString(resInfo,
									"comName", "")));
					logger.info("company Id is :" + resInfo.getInt("comId"));
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}

		} while (retry);
		return companyList;

	}

	// Loading Client details registered to one Company

	// Load Client SelectItem
	public List<SelectItem> loadPropertyList(int companyId, int clientId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> clientList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "SELECT cmPropertyName,cmId FROM ff_client_management_line left join "
						+ "ff_client on clClientId=clientId left join ff_client_management on  "
						+ "clCmId=cmId where clientRowStatus is null and (cmRowStatus='' or cmRowStatus is null)"
						+ "and cmComId=? and clClientId=?";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				prepStmnt.setInt(2, clientId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					clientList.add(new SelectItem("" + resInfo.getInt("cmId"),
							getString(resInfo, "cmPropertyName", "")));
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return clientList;
	}

	// Load Client SelectItem
	public List<SelectItem> loadClientList(int companyId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> clientList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select clientId, clientName from ff_client_company left join ff_client on ccClientId=clientId  "
						+ " where ccComId=? and clientRowStatus is null order by clientName";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					clientList.add(new SelectItem(""
							+ resInfo.getInt("clientId"), getString(resInfo,
							"clientName", "")));
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return clientList;
	}

	public List<ClientDTO> loadClientListDetails(int companyId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<ClientDTO> clientDTOList = new ArrayList<ClientDTO>();
		List<ClientPropertyDTO> clientPropertyDTOList = new ArrayList<ClientPropertyDTO>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select clientId,clientName,clientTitle,clientEmail,clientPhoneNumber,ccClientInviteMail from ff_client_company left join ff_client on ccClientId=clientId "
						+ " where ccComId=? and clientRowStatus is null order by clientEmail";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				int i = 1;
				while (resInfo.next()) {
					CompanyDAO companyDAO = new CompanyDAO();

					/* dose not require this company list */
					List<CompanyDTO> clientCompanyDTOList = new ArrayList<CompanyDTO>();
					// List<CompanyDTO> clientCompanyDTOList = companyDAO
					// .getClientComapnyDTOList(getInt(resInfo,
					// "clientId", 0));

					clientDTOList.add(new ClientDTO(i, resInfo
							.getInt("clientId"), getString(resInfo,
							"clientName", ""), getString(resInfo,
							"clientTitle", ""), getString(resInfo,
							"clientEmail", ""), getString(resInfo,
							"clientPhoneNumber", ""), clientCompanyDTOList,
							resInfo.getInt("ccClientInviteMail"), "", null, "",
							"", null, null, clientPropertyDTOList));
					i++;
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return clientDTOList;
	}

	// servlet request for Client Details for property
	public List<ClientDTO> loadClientDetailsForProperty(int companyId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<ClientDTO> clientDTOList = new ArrayList<ClientDTO>();
		List<ClientPropertyDTO> clientPropertyDTOList = new ArrayList<ClientPropertyDTO>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select * from ff_client_company left join ff_client on ccClientId=clientId "
						+ " where ccComId=? and clientRowStatus is null order by clientEmail";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				int i = 1;
				while (resInfo.next()) {
					CompanyDAO companyDAO = new CompanyDAO();
					List<CompanyDTO> clientCompanyDTOList = companyDAO
							.getClientComapnyDTOList(getInt(resInfo,
									"clientId", 0));

					clientDTOList.add(new ClientDTO(i, resInfo
							.getInt("clientId"), getString(resInfo,
							"clientName", ""), getString(resInfo,
							"clientTitle", ""), getString(resInfo,
							"clientEmail", ""), getString(resInfo,
							"clientPhoneNumber", ""), clientCompanyDTOList,
							resInfo.getInt("clientInviteMail"), "", null, "",
							"", null, null, clientPropertyDTOList));
					i++;
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return clientDTOList;
	}

	// Load InventoryProposal SelectItem
	public List<SelectItem> loadInventoryProposalList(int companyId,
			String creator) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> clientList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "Select iiId,igName,icName,iiName,iiLongDescription,iiShowToClient,"
						+ " igClientDescription,icClientDescription,icClientDescription,iiClientDescription,iiLongDescription from ff_inventory_item inner join ff_inventory_category on iiIcId=icId "
						+ " inner join ff_inventory_group on icIgId=igId "
						+ "where icComId=? and igRowStatus is null and icRowStatus is null and iiRowStatus is null and igType='Proposal Item' and  icVendorStatus is null and iiVendorStatus is null ";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					String icName = getString(resInfo, "icName", "");
					String selectItemValue = "";
					if (icName
							.equals(CommonVariables.WORK_ORDER_INVENTORY_CATEGORY_NAME)
							|| icName
									.equals(CommonVariables.INVENTORY_DEFAULT_CATEGORY_NAME)) {
						selectItemValue = getString(resInfo, "iiName", "")
								+ " - "
								+ getString(resInfo, "iiLongDescription", "");
					} else {
						selectItemValue = getString(resInfo, "icName", "")
								+ " - "
								+ getString(resInfo, "iiLongDescription", "");
					}
					if (creator.equals("")
							|| creator.equalsIgnoreCase("Foreman")) {
						clientList.add(new SelectItem(resInfo.getInt("iiId"),
								selectItemValue));
					} else {
						if (resInfo.getInt("iiShowToClient") == 1) {
							clientList.add(new SelectItem(resInfo
									.getInt("iiId"), getString(resInfo,
									"icClientDescription", "")
									+ " - "
									+ getString(resInfo, "iiLongDescription",
											"")));
						}
					}

				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return clientList;
	}

	// Load InventoryProposal SelectItem
	public List<SelectItem> loadInventoryGreenSheetList(int companyId,
			String creator) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> clientList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "Select iiId,igName,icName,iiName,iiLongDescription,iiShowToClient,"
						+ " igClientDescription,icClientDescription,icClientDescription,iiClientDescription,iiLongDescription from ff_inventory_item inner join ff_inventory_category on iiIcId=icId "
						+ " inner join ff_inventory_group on icIgId=igId "
						+ "where icComId=? and igRowStatus is null and icRowStatus is null and iiRowStatus is null and igType='Greensheet Item' and icVendorStatus is null and iiVendorStatus is null ";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					String icName = getString(resInfo, "icName", "");
					String selectItemValue = "";
					if (icName
							.equals(CommonVariables.WORK_ORDER_INVENTORY_CATEGORY_NAME)
							|| icName
									.equals(CommonVariables.INVENTORY_DEFAULT_CATEGORY_NAME)) {
						selectItemValue = getString(resInfo, "iiName", "")
								+ " - "
								+ getString(resInfo, "iiLongDescription", "");
					} else {
						selectItemValue = getString(resInfo, "icName", "")
								+ " - "
								+ getString(resInfo, "iiLongDescription", "");
					}
					if (creator.equals("")
							|| creator.equalsIgnoreCase("Foreman")) {
						clientList.add(new SelectItem(resInfo.getInt("iiId"),
								selectItemValue));
					} else {
						if (resInfo.getInt("iiShowToClient") == 1) {
							clientList.add(new SelectItem(resInfo
									.getInt("iiId"), getString(resInfo,
									"icClientDescription", "")
									+ " - "
									+ getString(resInfo, "iiLongDescription",
											"")));
						}
					}

				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return clientList;
	}

	// Load InventoryProposal SelectItem
	public List<SelectItem> loadInventoryMaintenanceList(int companyId,
			String creator) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> clientList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "Select iiId,igName,icName,iiName,iiLongDescription,iiShowToClient,"
						+ " igClientDescription,icClientDescription,icClientDescription,iiClientDescription,iiLongDescription"
						+ " from ff_inventory_item inner join ff_inventory_category on iiIcId=icId "
						+ " inner join ff_inventory_group on icIgId=igId "
						+ "where icComId=? and igRowStatus is null and icRowStatus is null and iiRowStatus is null and igType='Maintenance Item' and icVendorStatus is null and iiVendorStatus is null ";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					String icName = getString(resInfo, "icName", "");
					String selectItemValue = "";
					if (icName
							.equals(CommonVariables.WORK_ORDER_INVENTORY_CATEGORY_NAME)
							|| icName
									.equals(CommonVariables.INVENTORY_DEFAULT_CATEGORY_NAME)) {
						selectItemValue = getString(resInfo, "iiName", "")
								+ " - "
								+ getString(resInfo, "iiLongDescription", "");
					} else {
						selectItemValue = getString(resInfo, "icName", "")
								+ " - "
								+ getString(resInfo, "iiLongDescription", "");
					}
					if (creator.equals("")
							|| creator.equalsIgnoreCase("Foreman")) {
						clientList.add(new SelectItem(resInfo.getInt("iiId"),
								selectItemValue));
					} else {
						if (resInfo.getInt("iiShowToClient") == 1) {
							clientList.add(new SelectItem(resInfo
									.getInt("iiId"), getString(resInfo,
									"icClientDescription", "")
									+ " - "
									+ getString(resInfo, "iiLongDescription",
											"")));
						}
					}
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return clientList;
	}

	// Load InventoryProposal SelectItem
	public List<SelectItem> loadInventoryAssetList(int companyId, String creator) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> assetList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "Select iiId,igName,icName,iiName,iiLongDescription,iiShowToClient,"
						+ " igClientDescription,icClientDescription,icClientDescription,iiClientDescription,iiLongDescription from ff_inventory_item inner join ff_inventory_category on iiIcId=icId "
						+ " inner join ff_inventory_group on icIgId=igId "
						+ "where icComId=? and igRowStatus is null and icRowStatus is null and iiRowStatus is null and igType='Asset Item' and icVendorStatus is null and iiVendorStatus is null ";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					String icName = getString(resInfo, "icName", "");
					String selectItemValue = "";
					if (icName
							.equals(CommonVariables.WORK_ORDER_INVENTORY_CATEGORY_NAME)
							|| icName
									.equals(CommonVariables.INVENTORY_DEFAULT_CATEGORY_NAME)) {
						selectItemValue = getString(resInfo, "iiName", "")
								+ " - "
								+ getString(resInfo, "iiLongDescription", "");
					} else {
						selectItemValue = getString(resInfo, "icName", "")
								+ " - "
								+ getString(resInfo, "iiLongDescription", "");
					}
					if (creator.equals("")
							|| creator.equalsIgnoreCase("Foreman")) {
						assetList.add(new SelectItem(resInfo.getInt("iiId"),
								selectItemValue));
					} else {
						if (resInfo.getInt("iiShowToClient") == 1) {
							assetList.add(new SelectItem(
									resInfo.getInt("iiId"), getString(resInfo,
											"icClientDescription", "")
											+ " - "
											+ getString(resInfo,
													"iiLongDescription", "")));
						}
					}

				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return assetList;
	}
	
	//v1.21 starts
	// Load AssetLineItemName SelectedItem
		public List<SelectItem> loadWOAssetList(int companyId) {
			DBConnector db2Connector = DBConnector.getInstance();
			boolean retry;
			int numOfretry = 0;
			Connection con = null;
			PreparedStatement prepStmnt = null;
			ResultSet resInfo = null;
			List<SelectItem> assetList = new ArrayList<SelectItem>();
			do {
				retry = false;
				try {
					con = db2Connector.getConnection(true);

					String selectStatement = "select distinct(a.assetLineItemName) from ff_workorder_header a,ff_asset_management_line b "
							+ "where a.wohComId = ? and b.alId = a.assetLineItemId and a.wohRowStatus is null and a.assetLineItemName is not null";

					prepStmnt = con.prepareStatement(selectStatement);
					prepStmnt.setInt(1, companyId);
					resInfo = prepStmnt.executeQuery();
					while (resInfo.next()) {
						assetList.add(new SelectItem(getString(resInfo, "assetLineItemName", "")));
					}
				} catch (SQLException scon) {
					logger.error("Select SQLException.." + scon + "  " + numOfretry);
					if (numOfretry < 2) {
						numOfretry++;
						try {
							close(con);
						} catch (Exception e) {
						}
						con = db2Connector.getConnection(true);
						retry = true;
					} else {
						retry = false;
						logger.error("Select Exception :" + scon.getMessage());
					}
				} finally {
					close(resInfo);
					close(prepStmnt);
					close(con);
				}
			} while (retry);
			return assetList;
		}
		//v1.21 ends
		
		//v1.22 starts
		// Load AssetLineItemName SelectedItem
		public List<SelectItem> loadWOReqAssetList( Integer clientId) {
			DBConnector db2Connector = DBConnector.getInstance();
			boolean retry;
			int numOfretry = 0;
			Connection con = null;
			PreparedStatement prepStmnt = null;
			ResultSet resInfo = null;
			List<SelectItem> assetList = new ArrayList<SelectItem>();
			do {
				retry = false;
				try {
					con = db2Connector.getConnection(true);
					//v1.24 starts
					/* String selectStatement = "SELECT distinct(assetLineItemName) FROM work_order_request_list_view LEFT JOIN ff_proposal_maintenance_status "
							+ "ON wohApprovalStatus=psId LEFT JOIN ff_client_management ON cmId=wohClientId LEFT JOIN ff_company_management ON "
							+ "cmComId=comId LEFT JOIN ff_user_management ON wohCompletedBy=usrId LEFT JOIN ff_client AS m ON "
							+ "m.clientId=cwhCreatorClientId  WHERE wohClientId in (select clCmId from ff_client_management_line where clClientId=?) "
							+ "and IF(cwhId>0,true,IF((wohSubmitted=1 or wohSubmitted=2),true,false)) and assetLineItemName is not null order by if(wohUpdatedTime IS NULL, "
							+ "IF(cwhDate is NULL,wohDate,cwhDate), wohUpdatedTime ) desc  "; */
					String selectStatement = "SELECT distinct(assetLineItemName),IF(assetLineItemName IS NULL or assetLineItemName = '', null, assetLineItemName) as a  "
							+ "FROM work_order_request_list_view LEFT JOIN ff_proposal_maintenance_status "
							+ "ON wohApprovalStatus=psId LEFT JOIN ff_client_management ON cmId=wohClientId LEFT JOIN ff_company_management ON "
							+ "cmComId=comId LEFT JOIN ff_user_management ON wohCompletedBy=usrId LEFT JOIN ff_client AS m ON "
							+ "m.clientId=cwhCreatorClientId  WHERE wohClientId in (select clCmId from ff_client_management_line where clClientId=?) "
							+ "and assetLineItemName is not null and assetLineItemName != '' and IF(cwhId>0,true,IF((wohSubmitted=1 or wohSubmitted=2),true,false)) order by if(wohUpdatedTime IS NULL, "
							+ "IF(cwhDate is NULL,wohDate,cwhDate), wohUpdatedTime ) desc ";
					//v1.24 ends
					prepStmnt = con.prepareStatement(selectStatement);
					prepStmnt.setInt(1, clientId);
					resInfo = prepStmnt.executeQuery();
					while (resInfo.next()) {
						assetList.add(new SelectItem(getString(resInfo, "assetLineItemName", "")));
					}
				} catch (SQLException scon) {
					logger.error("Select SQLException.." + scon + "  " + numOfretry);
					if (numOfretry < 2) {
						numOfretry++;
						try {
							close(con);
						} catch (Exception e) {
						}
						con = db2Connector.getConnection(true);
						retry = true;
					} else {
						retry = false;
							logger.error("Select Exception :" + scon.getMessage());
						}
				} finally {
					close(resInfo);
					close(prepStmnt);
					close(con);
				}
			} while (retry);
			return assetList;
		}
		//v1.22 ends		

	// get admin selected email bcc list
	public String getAdminBccEmail(int companyId) {
		String adminBccEmail = "";
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "select distinct usrEmail from ff_user_management where usrSendMail=1 and usrComId=? and usrRowStatus is null";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {

					adminBccEmail = adminBccEmail
							+ resInfo.getString("usrEmail") + ",";
				}
				if (adminBccEmail.contains(",")) {
					adminBccEmail = removeLastChar(adminBccEmail);
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return adminBccEmail;

	}

	private static String removeLastChar(String str) {
		return str.substring(0, str.length() - 1);
	}

	public List<SelectItem> loadCountryList() {
		List<SelectItem> countryList = new ArrayList<SelectItem>();

		List<String> country = CountryListDAO.getCountryList();
		for (int i = 0; i < country.size(); i++) {
			countryList.add(new SelectItem(country.get(i).toString(), ""
					+ country.get(i).toString()));
		}
		return countryList;
	}

	public List<SelectItem> loadStateList(String countryName) {
		List<SelectItem> stateList = new ArrayList<SelectItem>();

		try {
			List<String> countryList = CountryListDAO.getCountryList();
			for (int i = 0; i < countryList.size(); i++) {
				if ((countryList.get(i).toString()).equals(countryName)) {
					List<String> stateLt = CountryListDAO.getStateList();
					String stateFullname = stateLt.get(i + 1).toString();
					String[] state_arr = stateFullname.split("\\|");
					for (int k = 0; k < state_arr.length; k++) {
						stateList
								.add(new SelectItem(state_arr[k], state_arr[k]));
					}

				}
			}
		} catch (Exception e) {
			logger.error("Error in loadStateList " + e.getMessage());
		}
		return stateList;
	}

	public List<SelectItem> loadCountryCodeList() {
		List<SelectItem> countryCodeList = new ArrayList<SelectItem>();

		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		String[] locales = Locale.getISOCountries();

		for (String countryCode : locales) {
			Locale obj = new Locale("", countryCode);
			countryCodeList
					.add(new SelectItem(obj.getCountry(), obj
							.getDisplayCountry()
							+ "(+"
							+ phoneUtil.getCountryCodeForRegion(obj
									.getCountry()) + ")"));
		}
		return countryCodeList;
	}

	public HashMap<String, String> loadCountryCodeMap() {
		HashMap<String, String> countryCodeMap = new HashMap<String, String>();

		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		String[] locales = Locale.getISOCountries();

		for (String countryCode : locales) {
			Locale obj = new Locale("", countryCode);
			countryCodeMap
					.put(obj.getCountry(),
							obj.getDisplayCountry()
									+ "(+"
									+ phoneUtil.getCountryCodeForRegion(obj
											.getCountry()) + ")");
		}
		return countryCodeMap;
	}

	public List<SelectItem> loadUnitList() {
		List<SelectItem> unitList = new ArrayList<SelectItem>();

		unitList.add(new SelectItem("YARDS", "YARDS"));
		unitList.add(new SelectItem("TONS", "TONS"));
		unitList.add(new SelectItem("EACH", "EACH"));
		unitList.add(new SelectItem("LNFT", "LNFT"));
		unitList.add(new SelectItem("10' PIECE", "10' PIECE"));
		unitList.add(new SelectItem("20' PIECE", "20' PIECE"));
		unitList.add(new SelectItem("SQFT", "SQFT"));
		unitList.add(new SelectItem("FLATS", "FLATS"));
		unitList.add(new SelectItem("1 GAL.", "1 GAL."));
		unitList.add(new SelectItem("7 GAL.", "7 GAL."));
		unitList.add(new SelectItem("15 GAL.", "15 GAL."));
		unitList.add(new SelectItem("24\" BOX", "24\" BOX"));
		unitList.add(new SelectItem("36\" BOX", "36\" BOX"));
		unitList.add(new SelectItem("2 GAL.", "2 GAL."));
		unitList.add(new SelectItem("5 GAL.", "5 GAL."));
		unitList.add(new SelectItem("BAREROOT", "BAREROOT"));
		unitList.add(new SelectItem("COMP.", "COMP."));
		unitList.add(new SelectItem("KIT", "KIT"));
		unitList.add(new SelectItem("SQFT/LNFT", "SQFT/LNFT"));
		unitList.add(new SelectItem("2' PIECE", "2' PIECE"));
		unitList.add(new SelectItem("PER PALLET", "PER PALLET"));
		unitList.add(new SelectItem("EXTRA", "EXTRA"));
		unitList.add(new SelectItem("HOURS", "HOURS"));
		unitList.add(new SelectItem("CU. YARDS", "CU. YARDS"));
		unitList.add(new SelectItem("EA", "EA"));
		unitList.add(new SelectItem("MAN/HOUR", "MAN/HOUR"));
		unitList.add(new SelectItem("PAIR", "PAIR"));
		unitList.add(new SelectItem("PER TREE", "PER TREE"));
		unitList.add(new SelectItem("# PLANTS", "# PLANTS"));

		return unitList;
	}

	public static String getOpticColor(String colorCode) {
		String retColor = "000000";
		if (hex2Rgb(colorCode) < 383) {
			retColor = "ffffff";
		}
		return retColor;
	}

	public static Integer hex2Rgb(String colorStr) {
		try {
			return Integer.valueOf(colorStr.substring(0, 2), 16)
					+ Integer.valueOf(colorStr.substring(2, 4), 16)
					+ Integer.valueOf(colorStr.substring(4, 6), 16);
		} catch (Exception e) {
			logger.error("Exception color " + colorStr + " in hex2Rgb :", e);
		}
		return 0;
	}

	/**
	 * Decode string to image
	 * 
	 * @param imageString
	 *            The string to decode
	 * @return decoded image
	 */
	public static BufferedImage decodeToImage(String imageString) {
		BufferedImage image = null;
		byte[] imageByte;
		try {
			BASE64Decoder decoder = new BASE64Decoder();
			imageByte = decoder.decodeBuffer(imageString);
			ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
			image = ImageIO.read(bis);
			bis.close();
		} catch (Exception e) {
		}
		return image;
	}

	/**
	 * Encode image to string
	 * 
	 * @param image
	 *            The image to encode
	 * @param type
	 *            jpeg, bmp, ...
	 * @return encoded string
	 */

	public String encodeToString2(BufferedImage image, String type) {
		String imageString = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			//MVIR Starts
			//ImageIO.write(image, type, bos);
			image = CommonDAO.getFormattedImage(image);
			ImageIO.write(image, "jpg", bos);
			//MVIR Ends

			byte[] imageBytes = bos.toByteArray();

			BASE64Encoder encoder = new BASE64Encoder();
			imageString = encoder.encode(imageBytes);
			bos.close();

		} catch (IOException e) {
			logger.error(e.toString());
		}
		return imageString;
	}

	public static String encodeToString(BufferedImage image, String type) {
		String imageString = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			//MVIR Starts
			//ImageIO.write(image, type, bos);
			image = getFormattedImage(image);
			ImageIO.write(image, "jpg", bos);
			//MVIR Ends
			byte[] imageBytes = bos.toByteArray();
			BASE64Encoder encoder = new BASE64Encoder();
			imageString = encoder.encode(imageBytes);
			bos.close();
		} catch (IOException e) {

		}
		return imageString;
	}

	public static String getServerTimeZone() {
		try {
			Calendar cal = Calendar.getInstance();
			// logger.info("get server time zone : " +
			// cal.getTimeZone().getID());
			return cal.getTimeZone().getID();
		} catch (Exception e) {
			logger.error("Cannot determine timezone of the server", e);
		}
		return "";
	}

	public static synchronized String getDateForTimeZoneAsString(Date dbDate,
			String timezoneId) {

		DateTimeFormatter fmt = DateTimeFormat.forPattern(dateTimeFormat);
		DateTime origDate = new DateTime(dbDate);
		DateTime dtTz = origDate.withZone(DateTimeZone.forID(timezoneId));
		String dateForTz = dtTz.toString(fmt);
		return dateForTz;

		// SimpleDateFormat format = new SimpleDateFormat(
		// dateTimeFormat);
		// Calendar conertedTime = new GregorianCalendar(
		// TimeZone.getTimeZone(timezoneId));
		// try {
		// Calendar localTime = new GregorianCalendar(
		// TimeZone.getTimeZone(getServerTimeZone()));
		// localTime.setTime(dbDate);
		// conertedTime.setTimeInMillis(localTime.getTimeInMillis());
		// } catch (Exception e) {
		// // System.out.println("====== Error Date =====" + e.getMessage());
		// }
		// String dateForTz = format.format(conertedTime);
		// return dateForTz;
	}

	public static synchronized String getDateOnlyForTimeZoneAsString(
			Date dbDate, String timezoneId) {

		DateTimeFormatter fmt = DateTimeFormat.forPattern(dateFormatEamil);
		DateTime origDate = new DateTime(dbDate);
		DateTime dtTz = origDate.withZone(DateTimeZone.forID(timezoneId));
		String dateForTz = dtTz.toString(fmt);
		return dateForTz;
	}

	public static Date getDateForTimeZoneAsDate(Date dbDate, String timezoneId) {
		DateTime origDate = new DateTime(dbDate);
		DateTime dtTz = origDate.withZone(DateTimeZone.forID(timezoneId));
		return dtTz.toDate();
	}

	public static Date getDateFromTimeZoneToTimezone(Date dbDate,
			String fromTimezone, String toTimezone) {
		Calendar conertedTime = new GregorianCalendar(
				TimeZone.getTimeZone(toTimezone));
		try {

			Calendar localTime = new GregorianCalendar(
					TimeZone.getTimeZone(fromTimezone));
			localTime.setTime(dbDate);

			conertedTime.setTimeInMillis(localTime.getTimeInMillis());
		} catch (Exception e) {
			logger.error("====== Error Date =====" + e.getMessage());
		}
		return conertedTime.getTime();
	}

	public Date getTimeFromTimeZoneToTimezone(Date dbDate, String fromTimezone,
			String toTimezone) {
		Calendar conertedTime = new GregorianCalendar(
				TimeZone.getTimeZone(toTimezone));
		try {

			Calendar localTime = new GregorianCalendar(
					TimeZone.getTimeZone(fromTimezone));
			localTime.set(Calendar.HOUR_OF_DAY, dbDate.getHours());
			localTime.set(Calendar.MINUTE, dbDate.getMinutes());
			localTime.set(Calendar.SECOND, dbDate.getSeconds());

			conertedTime.setTimeInMillis(localTime.getTimeInMillis());
		} catch (Exception e) {
			logger.error("====== Error Date =====" + e.getMessage());
		}
		return conertedTime.getTime();
	}

	public static Date getSelectedDateStartTime(Date dbDate) {
		Calendar conertedTime = new GregorianCalendar(
				TimeZone.getTimeZone(getServerTimeZone()));
		try {

			Calendar localTime = new GregorianCalendar(
					TimeZone.getTimeZone(UserUtil.getUserTimezone()));
			localTime.setTime(dbDate);
			localTime.set(Calendar.HOUR_OF_DAY, 0);
			localTime.set(Calendar.MINUTE, 0);
			localTime.set(Calendar.SECOND, 0);

			conertedTime.setTimeInMillis(localTime.getTimeInMillis());
		} catch (Exception e) {
			// System.out.println("====== Error Date =====" + e.getMessage());
		}
		return conertedTime.getTime();
	}

	public static Date addDate(Date dbDate, Integer addVal) {
		try {
			Calendar c = Calendar.getInstance();
			c.setTime(dbDate);
			c.add(Calendar.DATE, addVal);
			dbDate = c.getTime();
		} catch (Exception e) {
			// System.out.println("====== Error Date =====" + e.getMessage());
		}
		return dbDate;
	}

	public static Date addMonth(Date dbDate, Integer addVal) {
		try {
			Calendar c = Calendar.getInstance();
			c.setTime(dbDate);
			c.add(Calendar.MONTH, addVal);
			dbDate = c.getTime();
		} catch (Exception e) {
			// System.out.println("====== Error Date =====" + e.getMessage());
		}
		return dbDate;
	}

	public static Date getYTDDate(Date dbDate) {
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.YEAR, dbDate.getYear());
			calendar.set(Calendar.DATE, 1);
			calendar.set(Calendar.MONTH, 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			dbDate = calendar.getTime();
		} catch (Exception e) {
			// System.out.println("====== Error Date =====" + e.getMessage());
		}
		return dbDate;
	}

	// find last week
	public static Date findPreviousWeekDate(Date dbDate) {

		try {
			Calendar c = Calendar.getInstance();
			c.setTime(dbDate);
			c.add(Calendar.WEEK_OF_MONTH, -1);
			dbDate = c.getTime();
		} catch (Exception e) {
			// System.out.println("====== Error Date =====" + e.getMessage());
		}
		return dbDate;
	}

	// find Month first Date
	public static Date getMonthFirstDate(Date dbDate) {

		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dbDate);
			cal.set(Calendar.DATE, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			dbDate = cal.getTime();
		} catch (Exception e) {
			// System.out.println("====== Error Date =====" + e.getMessage());
		}
		return dbDate;

	}

	// find Month Last Date
	public static Date getMonthLastDate(Date dbDate) {

		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dbDate);
			cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			dbDate = cal.getTime();

		} catch (Exception e) {
			// System.out.println("====== Error Date =====" + e.getMessage());
		}
		return dbDate;
	}

	public static Date getSunday(Date today) {
		Calendar cal = Calendar.getInstance();

		cal.setTime(today);

		int dow = cal.get(Calendar.DAY_OF_WEEK);

		while (dow != Calendar.SUNDAY) {
			int date = cal.get(Calendar.DATE);

			int month = cal.get(Calendar.MONTH);

			int year = cal.get(Calendar.YEAR);

			if (date == getMonthLastDate(month, year)) {

				if (month == Calendar.DECEMBER) {
					month = Calendar.JANUARY;

					cal.set(Calendar.YEAR, year + 1);
				} else {
					month++;
				}

				cal.set(Calendar.MONTH, month);

				date = 1;
			} else {
				date++;
			}

			cal.set(Calendar.DATE, date);

			dow = cal.get(Calendar.DAY_OF_WEEK);
		}

		return cal.getTime();
	}

	public static int getMonthLastDate(int month, int year) {
		switch (month) {
		case Calendar.JANUARY:
		case Calendar.MARCH:
		case Calendar.MAY:
		case Calendar.JULY:
		case Calendar.AUGUST:
		case Calendar.OCTOBER:
		case Calendar.DECEMBER:
			return 31;

		case Calendar.APRIL:
		case Calendar.JUNE:
		case Calendar.SEPTEMBER:
		case Calendar.NOVEMBER:
			return 30;

		default: // Calendar.FEBRUARY
			return year % 4 == 0 ? 29 : 28;
		}
	}

	public static boolean getContainsOfLatLngBounds(LatLngBounds base,
			LatLngBounds compare) {
		boolean retValue = false;
		try {
			LatLng sw = base.getSouthWest();
			LatLng ne = base.getNorthEast();
			LatLng sw2 = compare.getSouthWest();
			LatLng ne2 = compare.getNorthEast();

			// System.out.println("== (sw2.getLat() >= sw.getLat()) ="
			// + sw2.getLat() + " - " + sw.getLat() + "="
			// + (sw2.getLat() >= sw.getLat()));
			// System.out.println("== (ne2.getLat() <= ne.getLat()) ="
			// + ne2.getLat() + " - " + ne.getLat() + "="
			// + (ne2.getLat() <= ne.getLat()));
			// System.out.println("== (sw2.getLng() >= sw.getLng()) ="
			// + sw2.getLng() + " - " + sw.getLng() + "="
			// + (sw2.getLng() >= sw.getLng()));
			// System.out.println("== (ne2.getLng() <= ne.getLng()) ="
			// + ne2.getLng() + " - " + ne.getLng() + "="
			// + (ne2.getLng() <= ne.getLng()));

			// retValue = ((sw2.getLat() >= sw.getLat())
			// && (ne2.getLat() <= ne.getLat())
			// && (sw2.getLng() >= sw.getLng()) && (ne2.getLng() <= ne
			// .getLng()));

			retValue = ((round(sw2.getLat(), 5) >= round(sw.getLat(), 5))
					&& (round(ne2.getLat(), 5) <= round(ne.getLat(), 5))
					&& (round(sw2.getLng(), 5) >= round(sw.getLng(), 5)) && (round(
					ne2.getLng(), 5) <= round(ne.getLng(), 5)));
		} catch (Exception e) {

		}
		return retValue;
	}

	public static boolean getContainsOfSelectedPoint(LatLngBounds base,
			LatLng compare) {
		boolean retValue = false;
		try {
			LatLng sw = base.getSouthWest();
			LatLng ne = base.getNorthEast();
			LatLng sw2 = compare;
			LatLng ne2 = compare;

			retValue = ((sw2.getLat() >= sw.getLat())
					&& (ne2.getLat() <= ne.getLat())
					&& (sw2.getLng() >= sw.getLng()) && (ne2.getLng() <= ne
					.getLng()));

		} catch (Exception e) {
			logger.error("", e);
		}
		return retValue;
	}

	// public static LatLngBounds getLatLngBoundsFromCLientDTO(
	// ClientPropertyDTO clientPropertyDTO) {
	// LatLngBounds bounds = null;
	// try {
	// String northEastArray[] = clientPropertyDTO
	// .getClientMapNorthEastAddress().split(",");
	// LatLng northEast = new LatLng(Double.valueOf(northEastArray[0]),
	// Double.valueOf(northEastArray[1]));
	// String southWestArray[] = clientPropertyDTO
	// .getClientMapSouthWestAddress().split(",");
	// LatLng southWest = new LatLng(Double.valueOf(southWestArray[0]),
	// Double.valueOf(southWestArray[1]));
	// bounds = new LatLngBounds(northEast, southWest);
	// } catch (Exception e) {
	// System.out.println("getLatLngBoundsFromCLientDTO : " + e);
	// }
	// return bounds;
	// }

	public LatLngBounds getLatLngBounds(String clientMapNorthEastAddress,
			String clientMapSouthWestAddress) {
		LatLngBounds bounds = null;
		try {
			String northEastArray[] = clientMapNorthEastAddress.split(",");
			LatLng northEast = new LatLng(Double.valueOf(northEastArray[0]),
					Double.valueOf(northEastArray[1]));
			String southWestArray[] = clientMapSouthWestAddress.split(",");
			LatLng southWest = new LatLng(Double.valueOf(southWestArray[0]),
					Double.valueOf(southWestArray[1]));
			bounds = new LatLngBounds(northEast, southWest);
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return bounds;
	}

	public static double round(double value, int places) {
		if (places < 0)
			return 0;

		long factor = (long) Math.pow(10, places);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}

	// password random generator

	public String generateRandomPassword() {
		int i = 0;
		String alphabetCaps = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		String numeric = "123456789";
		String symbols = "#@$&";
		String passsword = "";
		while (i < 2) {
			int randCaps = (int) (Math.random() * alphabetCaps.length());
			int randAlph = (int) (Math.random() * alphabet.length());
			int randNo = (int) (Math.random() * numeric.length());
			int randSym = (int) (Math.random() * symbols.length());

			passsword = passsword + alphabetCaps.charAt(randCaps)
					+ alphabet.charAt(randAlph) + numeric.charAt(randNo)
					+ symbols.charAt(randSym);

			++i;
		}
		// System.out.print("Password : " + passsword);
		return passsword;
	}

	/* get company logo by company id for email proposal and maintenane */
	public String getCompanyLogoById(int companyId) {
		String imageString = "";
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "select comLogo from ff_company_management where comId=? and comRowStatus is null";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				if (resInfo.next()) {
					InputStream is = resInfo.getBinaryStream("comLogo");
					if (is != null) {
						try {

							BufferedImage img = ImageIO.read(is);
							imageString = "data:image/png;base64,"
									+ encodeToString(img, "png");

						} catch (Exception e) {
							imageString = "";
						} finally {
							if (is != null) {
								try {
									is.close();
								} catch (IOException e) {
								}
							}
						}
					}
				} else {
					imageString = "";
				}

			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return imageString;

	}

	/**
	 * Get thumbnail company logo string by company id.
	 * 
	 * @param companyId
	 * @return
	 */
	public String getCompanyThumbnailLogoStringById(int companyId) {
		String imageString = "";
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select comThumbnailLogo from ff_company_management where comRowStatus is null and comId=?";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				if (resInfo.next()) {
					InputStream is = resInfo
							.getBinaryStream("comThumbnailLogo");
					if (is != null) {
						try {

							BufferedImage img = ImageIO.read(is);
							imageString = "data:image/png;base64,"
									+ encodeToString(img, "png");

						} catch (Exception e) {
							imageString = "";
						} finally {
							if (is != null) {
								try {
									is.close();
								} catch (IOException e) {
								}
							}
						}
					}
				} else {
					imageString = "";
				}

			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return imageString;

	}

	// check duplicate name
	public boolean isExistsUserNameByCompanyId(int companyId, int userId,
			String userName) {

		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		boolean dupsUserName = false;
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select * from ff_user_management "
						+ "where usrComId=? and usrId!=? and usrName=? and usrRowStatus is null and (usrRole=? or usrRole=?)";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				prepStmnt.setInt(2, userId);
				prepStmnt.setString(3, userName);
				prepStmnt.setString(4, "Admin");
				prepStmnt.setString(5, "Super Admin");
				resInfo = prepStmnt.executeQuery();
				// int i = 1;

				if (resInfo.next()) {

					dupsUserName = true;
					// i = i + 1;
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}

					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return dupsUserName;
	}

	public byte[] getCompnayLogoInBytes(String imageString) {

		byte[] headerImage = null;
		try {

			String imageDataBytes = imageString.substring(imageString
					.indexOf(",") + 1);
			BufferedImage buffImage = decodeToImage(imageDataBytes);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			//MVIR Starts
			//ImageIO.write(buffImage, "png", os);
			buffImage = getFormattedImage(buffImage);
			ImageIO.write(buffImage, "jpg", os);
			//MVIR Ends
			InputStream is = new ByteArrayInputStream(os.toByteArray());
			BufferedImage img = ImageIO.read(is);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			//MVIR Starts
			//ImageIO.write(img, "png", bos);
			img = getFormattedImage(img);
			ImageIO.write(img, "jpg", bos);
			//MVIR Ends
			headerImage = bos.toByteArray();
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
			headerImage = null;
		}

		return headerImage;
	}

	public boolean isExistsForemanNameByCompanyId(int companyId, int userId,
			String userName) {

		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		boolean dupsUserName = false;
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select usrId from ff_user_management "
						+ "where usrRowStatus is null and usrComId=? and usrId!=? and usrName=? and (usrRole='Foreman' OR usrRole='Super Admin')";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				prepStmnt.setInt(2, userId);
				prepStmnt.setString(3, userName);
				resInfo = prepStmnt.executeQuery();
				// int i = 1;

				if (resInfo.next()) {

					dupsUserName = true;
					// i = i + 1;
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}

					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return dupsUserName;
	}

	// public void setSession(String sessionName, String sessionValue) {
	// FacesContext fc = FacesContext.getCurrentInstance();
	// HttpSession session = (HttpSession) fc.getExternalContext().getSession(
	// true);
	// session.setAttribute(sessionName, sessionValue);
	// }
	//
	// public String getSession(String sessionName) {
	// String value = "";
	// FacesContext fc = FacesContext.getCurrentInstance();
	// HttpSession session = (HttpSession) fc.getExternalContext().getSession(
	// true);
	// value = session.getAttribute(sessionName).toString();
	//
	// return value;
	// }

	public String getSetSession(String setSessionName, String setSessionValue,
			String getSessionValue) {
		String value = "";

		FacesContext fc = FacesContext.getCurrentInstance();
		HttpSession session = null;

		try {
			
			/**
			 * This condition is used to return the timezone
			 */
			
			if(fc==null && StringUtils.isNotEmpty(getSessionValue) && ("loginTimeZone".equalsIgnoreCase(getSessionValue))) {
				return CommonDAO.getServerTimeZone();
			}
			
			session = (HttpSession) fc.getExternalContext().getSession(
					true);

			if (getSessionValue != null && getSessionValue.trim().length() > 1) {
				value = session.getAttribute(getSessionValue).toString();
			}

			if (setSessionName != null && setSessionName.trim().length() > 1
					&& setSessionValue != null) {

				session.setAttribute(setSessionName, setSessionValue);

			}
			// if (setSessionValue.equals("")) {
			// session.setAttribute(setSessionName, setSessionValue);
			// }

		} catch (Exception e) {
			logger.error("Exception in get set session");
		}
		return value;
	}

	public String forgetPasswordGeneration(String usrEmail, Integer usrId,
			String usrName, String usrRole) {
		String msg = "";
		String resetPasswordLink = "";
		String forgotPasswordLink = "";
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date currentDateTime = new Date();
			// passing details through url parameter
			JSONObject obj = new JSONObject();
			obj.put("UsrEmail", usrEmail);
			obj.put("UsrId", usrId);
			obj.put("UsrName", usrName);
			obj.put("UsrRole", usrRole);
			obj.put("CurrentDate", dateFormat.format(currentDateTime));

			// token encryption
			AESEncryption encryption = new AESEncryption();
			String encryptionStr = encryption.encrypt(obj.toString());

			resetPasswordLink = getURLPath() + "/reset_password.xhtml?token="
					+ encryptionStr;

			forgotPasswordLink = getURLPath()
					+ "/forgot_password.xhtml?email_id=" + usrEmail;

			msg = MessageLoader.getInstance().getMessageStatement(
					"EMAIL_DIV_TAG")
					+ "Forgot your password? <a href=\""
					+ forgotPasswordLink
					+ "\">CLICK HERE</a> to reset it.  </div><br/>";

			msg = msg
					+ MessageLoader.getInstance().getMessageStatement(
							"EMAIL_DIV_TAG")
					+ "Don't have an account? <a href=\""
					+ forgotPasswordLink
					+ "\">CLICK HERE</a> to reset password. "
					+ "The instructions for resetting password will be delivered to your email. "
					+ "You can then login using your email (" + usrEmail
					+ ") and the password you just reset.  </div><br/>";

		} catch (Exception e) {

		}
		return msg;
	}

	public String emailTermsAndConditionsContent(String urlName) {
		String msg = "";
		try {

			String termsAndConditions = "<a href=\" "
					+ urlName
					+ "/terms_and_conditions.xhtml \">Terms and Conditions</a> ";
			String privacyPolicy = "<a href=\" " + urlName
					+ "/privacy_policy.xhtml \">Privacy Policy</a> ";

			msg = msg
					+ MessageLoader.getInstance().getMessageStatement(
							"EMAIL_DIV_TAG_FOOTER")
					+ "<br/>All actions taken with this email are subject to "
					+ termsAndConditions + " and " + privacyPolicy + ".</div>";
		} catch (Exception e) {

		}
		return msg;
	}

	public String getTechnicalSupportContent(String urlName, String userEmail) {

		String loginURL = " <a href=\"" + urlName + "/login?email_id="
				+ userEmail + " \"   >login</a>   ";
		String homePageURL = " <a href=\"" + urlName
				+ "/homepage.xhtml \"   >Cappsure</a>   ";
		String msg = MessageLoader.getInstance().getMessageStatement(
				"EMAIL_DIV_TAG")
				+ "With service or sale related questions please call us directly at <b>(702) 795-0300</b>. "
				+ "For Technical Support please contact <a href=\"mailto:customersupport@cappsure.com\">customersupport@cappsure.com</a> . "
				+ "You can also "
				+ loginURL
				+ " to "
				+ homePageURL
				+ " and see all your reports! </div>";

		return msg;
	}

	public String emailTrackingCode(Integer clientId, Integer companyId,
			String trackFor) {
		String trackingCode = "";
		try {

			trackingCode = "https://www.google-analytics.com/collect?v=1&tid="
					+ getTrackingCompanyGAId(companyId) + "&cid=" + clientId
					+ "&t=event&dp=/email/&ec=email&ea=open&el=" + trackFor
					+ "&cs=notification&cm=email&cn=open_email_tracking";

		} catch (Exception e) {
		}
		return trackingCode;
	}

	public static String getURLPath() {
		String urlPath = "";
		try {
			// HttpServletRequest req = (HttpServletRequest) FacesContext
			// .getCurrentInstance().getExternalContext().getRequest();
			// urlPath = req.getRequestURL().toString();
			// int index = urlPath.lastIndexOf('/');
			// urlPath = urlPath.substring(0, index) + "";
			// urlPath = urlPath.replace("javax.faces.resource/images/", "");
			// urlPath = urlPath.replace("javax.faces.resource/gmap/", "");

			urlPath = MessageLoader.getInstance().getMessageStatement(
					"MSG_EMAIL_URL_LINK");
		} catch (Exception e) {
		}
		return urlPath;
	}

	public VelocityEngine getVelocityProperty(String sourcePath) {
		VelocityEngine velocityEngine = new VelocityEngine();
		try {
			sourcePath = CommonDAO.class.getProtectionDomain().getCodeSource()
					.getLocation().toURI().getPath();
			sourcePath = sourcePath.replaceAll("(?=WEB-INF)(.*)", "");

			Properties properties = new Properties();
			properties.setProperty("resource.loader", "file");
			properties
					.setProperty("class.resource.loader.class",
							"org.apache.velocity.runtime.resource.loader.FileResourceLoader");
			properties.setProperty("file.resource.loader.path", sourcePath
					+ "resources/emailtemplate/");
			velocityEngine.init(properties);
		} catch (Exception e) {
			logger.error("Exception in get Velocity Property ", e);
		}
		return velocityEngine;

	}

	public static String getSourceFilePath() {
		String sourcePath = null;
		try {
			ServletContext ctx = (ServletContext) FacesContext
					.getCurrentInstance().getExternalContext().getContext();
			sourcePath = ctx.getRealPath("/");

		} catch (Exception e) {
			logger.error("Exeption in get source fiel path from servlet : ", e);
		}

		try {
			if (sourcePath == null || sourcePath.trim().length() < 0) {

				sourcePath = CommonDAO.class.getProtectionDomain()
						.getCodeSource().getLocation().toURI().getPath();
				sourcePath = sourcePath.replaceAll("(?=WEB-INF)(.*)", "");

			}
		} catch (Exception e) {
			logger.error("Exception in Get source file path from class ", e);
		}

		logger.info("source path : " + sourcePath);

		return sourcePath;
	}

	public PreferencesValueDTO assignPrefrencePDFValue(
			List<PreferencesDTO> preferencesList) {
		PreferencesValueDTO preferencesValueDTO = new PreferencesValueDTO(1);

		for (PreferencesDTO dto : preferencesList) {

			if (dto.getPreferencePrefix().equals("GENERATE_PROPOSAL_PDF")) {
				preferencesValueDTO.setEnableProposalPDF(dto
						.isPreferenceEnable());
			}
		}
		return preferencesValueDTO;
	}

	public PreferencesValueDTO assignPrefrenceValue(
			List<PreferencesDTO> preferencesList) {
		PreferencesValueDTO preferencesValueDTO = new PreferencesValueDTO(1);

		for (PreferencesDTO dto : preferencesList) {

			if (dto.getPreferencePrefix().equals("GENERATE_PROPOSAL_PDF")) {
				preferencesValueDTO.setEnableProposalPDF(dto
						.isPreferenceEnable());
			}

			// Automatically Invite client email
			if (dto.getPreferencePrefix().equals(
					"AUTOMATIC_INVITE_CLIENT_EMAIL")) {

				preferencesValueDTO.setInviteClientEmail(dto
						.isPreferenceEnable());
			}

			if (dto.getPreferencePrefix().equals(
					"APPROVED_PROPOSAL_FOREMAN_INCENTIVE")) {
				preferencesValueDTO.setProposalForemanIncentive(dto
						.getPreferenceValue());

				if (dto.isPreferenceIncentiveEnable() == true) {
					preferencesValueDTO.setEnableForemanIncentive(dto
							.isPreferenceIncentiveEnable());
				}

				preferencesValueDTO.setForemanIncentiveApproval(dto
						.getPreferenceIncentiveApproval());

			}
			if (dto.getPreferenceValue().length() >= 1) {

				if (dto.getPreferencePrefix().equals("PROPOSAL_REMIND_TIMES")) {
					preferencesValueDTO.setProposalRemindTimes(Integer
							.valueOf(dto.getPreferenceValue()));
				}

				if (dto.getPreferencePrefix().equals(
						"PRPOSAL_REMIND_BETWEEN_DAYS")) {
					preferencesValueDTO.setPrposalRemindBetweenDays(Integer
							.valueOf(dto.getPreferenceValue()));
				}

				if (dto.getPreferencePrefix().equals(
						"PROPOSAL_REMIND_MAIL_TIME")) {

					try {

						SimpleDateFormat displayFormat = new SimpleDateFormat(
								CommonDAO.dateTimeFormat);

						SimpleDateFormat dbFormat = new SimpleDateFormat(
								CommonDAO.dateTimeFormatSQL);
						Date date = null;
						if (dto.getPreferenceValue() != null) {
							date = displayFormat.parse(CommonDAO
									.getDateForTimeZoneAsString(dbFormat
											.parse(dto.getPreferenceValue()),
											UserUtil.getUserTimezone(false)));

						}

						preferencesValueDTO.setProposalRemindMailTime(date);

					} catch (Exception e) {
						logger.error("Date Time is empty");
					}
				}
				if (dto.getPreferencePrefix().equals("PROPOSAL_REMIND_TIMES")
						|| dto.getPreferencePrefix().equals(
								"PRPOSAL_REMIND_BETWEEN_DAYS")
						|| dto.getPreferencePrefix().equals(
								"PROPOSAL_REMIND_MAIL_TIME")) {
					if (dto.isPreferenceEnable() == true) {
						preferencesValueDTO.setEnablePrposalRemind(dto
								.isPreferenceEnable());
					}
				}

				if (dto.getPreferencePrefix().equals("COMPANY_EMAIL_PREFIX")) {
					preferencesValueDTO.setCompanyEmailPrefix(dto
							.getPreferenceValue());
				}

				//
				if (dto.getPreferencePrefix().equals("ENABLE_QUICKBOOKS")) {
					boolean enableQuickBooks = false;
					if (dto.getPreferenceValue().equals("1")) {
						enableQuickBooks = true;
					}
					preferencesValueDTO
							.setEnableQuickBooksRemind(enableQuickBooks);
				}

				if (dto.getPreferencePrefix().equals(
						"QUICKBOOKS_REMIND_BETWEEN_DAYS")) {
					try {
						preferencesValueDTO
								.setQuickBooksRemindBetweenDays(Integer
										.valueOf(dto.getPreferenceValue()));
					} catch (Exception e) {
						logger.error(
								"Exception in get QUICKBOOKS_REMIND_BETWEEN_DAYS : ",
								e);
					}
				}

				if (dto.getPreferencePrefix().equals(
						"QUICKBOOKS_TRANSACTION_TYPE")) {
					try {
						preferencesValueDTO.setQuickBooksTransactionType(dto
								.getPreferenceValue());
					} catch (Exception e) {
						logger.error(
								"Exception in get QUICKBOOKS_TRANSACTION_TYPE : ",
								e);
					}
				}

				if (dto.getPreferencePrefix().equals("QUICKBOOKS_MODULE")) {
					try {
						preferencesValueDTO.setQuickBooksModuleType(dto
								.getPreferenceValue());
					} catch (Exception e) {
						logger.error("Exception in get QUICKBOOKS_MODULE : ", e);
					}
				}

				if (dto.getPreferencePrefix().equals("QUICKBOOKS_REMIND_TIME")) {

					try {

						SimpleDateFormat displayFormat = new SimpleDateFormat(
								CommonDAO.dateTimeFormat);

						SimpleDateFormat dbFormat = new SimpleDateFormat(
								CommonDAO.dateTimeFormatSQL);
						Date date = null;
						if (dto.getPreferenceValue() != null) {
							date = displayFormat.parse(CommonDAO
									.getDateForTimeZoneAsString(dbFormat
											.parse(dto.getPreferenceValue()),
											UserUtil.getUserTimezone(false)));

						}

						preferencesValueDTO.setQuickBooksRemindTime(date);

					} catch (Exception e) {
						logger.error("Date Time is empty");
					}
				}

				//
			}

		}

		return preferencesValueDTO;
	}

	public PreferencesValueDTO assignPrefrenceValueFromServlet(
			List<PreferencesDTO> preferencesList) {
		PreferencesValueDTO preferencesValueDTO = new PreferencesValueDTO(1);

		for (PreferencesDTO dto : preferencesList) {

			if (dto.getPreferencePrefix().equals("GENERATE_PROPOSAL_PDF")) {
				if (dto.isPreferenceEnable() == true) {
					preferencesValueDTO.setEnableProposalPDF(dto
							.isPreferenceEnable());

				}
			}

			if (dto.getPreferencePrefix().equals("PROPOSAL_REMIND_TIMES")) {
				try {
					preferencesValueDTO.setProposalRemindTimes(Integer
							.valueOf(dto.getPreferenceValue()));
				} catch (Exception e) {
					logger.error("Remind Times Not set ");
				}
			}

			if (dto.getPreferencePrefix().equals("PRPOSAL_REMIND_BETWEEN_DAYS")) {
				try {
					preferencesValueDTO.setPrposalRemindBetweenDays(Integer
							.valueOf(dto.getPreferenceValue()));

				} catch (Exception e) {
					logger.error("Remind Times Between days Not set ");
				}
			}

			if (dto.getPreferencePrefix().equals("PROPOSAL_REMIND_MAIL_TIME")) {

				SimpleDateFormat formatter = new SimpleDateFormat(
						dateTimeFormatSQL);
				try {

					preferencesValueDTO.setProposalRemindMailTime(formatter
							.parse(dto.getPreferenceValue()));

				} catch (Exception e) {
					logger.error("Remind Mail Times Between days Not set ");
				}
			}
			if (dto.getPreferencePrefix().equals("PROPOSAL_REMIND_TIMES")
					|| dto.getPreferencePrefix().equals(
							"PRPOSAL_REMIND_BETWEEN_DAYS")
					|| dto.getPreferencePrefix().equals(
							"PROPOSAL_REMIND_MAIL_TIME")) {
				if (dto.isPreferenceEnable() == true) {
					preferencesValueDTO.setEnablePrposalRemind(dto
							.isPreferenceEnable());

				}
			}

			// Quickbooks
			if (dto.getPreferencePrefix().equals("ENABLE_QUICKBOOKS")) {
				boolean enableQuickBooks = false;
				logger.debug("VVVVVV : " + dto.getPreferenceValue());
				if (dto.getPreferenceValue().equals("1")) {
					enableQuickBooks = true;
				}
				preferencesValueDTO.setEnableQuickBooksRemind(enableQuickBooks);
			}

			if (dto.getPreferencePrefix().equals("QUICKBOOKS_MODULE")) {
				try {
					preferencesValueDTO.setQuickBooksModuleType(dto
							.getPreferenceValue());
				} catch (Exception e) {
					logger.error("error in get QUICKBOOKS_MODULE : ");
				}
			}

			if (dto.getPreferencePrefix().equals("QUICKBOOKS_REMIND_TIME")) {

				SimpleDateFormat formatter = new SimpleDateFormat(
						dateTimeFormatSQL);
				try {

					preferencesValueDTO.setQuickBooksRemindTime(formatter
							.parse(dto.getPreferenceValue()));

				} catch (Exception e) {
					logger.error("error in get QUICKBOOKS_REMIND_TIME : ");
				}

				// try {
				//
				// SimpleDateFormat displayFormat = new SimpleDateFormat(
				// CommonDAO.dateTimeFormat);
				//
				// SimpleDateFormat dbFormat = new SimpleDateFormat(
				// CommonDAO.dateTimeFormatSQL);
				// Date date = null;
				// if (dto.getPreferenceValue() != null) {
				// date = displayFormat.parse(CommonDAO
				// .getDateForTimeZoneAsString(dbFormat.parse(dto
				// .getPreferenceValue()), UserUtil
				// .getUserTimezone(false)));
				//
				// }
				//
				// preferencesValueDTO.setQuickBooksRemindTime(date);
				//
				// } catch (Exception e) {
				// logger.error("Date Time is empty");
				// }
			}

			if (dto.getPreferencePrefix().equals(
					"QUICKBOOKS_REMIND_BETWEEN_DAYS")) {
				try {
					preferencesValueDTO
							.setQuickBooksRemindBetweenDays(Integer
									.valueOf(dto.getPreferenceValue().trim()
											.equals("") ? "0" : dto
											.getPreferenceValue().trim()));
				} catch (Exception e) {
					logger.error("error in get QUICKBOOKS_REMIND_BETWEEN_DAYS : ");
				}
			}

			if (dto.getPreferencePrefix().equals("QUICKBOOKS_TRANSACTION_TYPE")) {
				try {
					preferencesValueDTO.setQuickBooksTransactionType(dto
							.getPreferenceValue());
				} catch (Exception e) {
					logger.error("error in get BQUICKBOOKS_TRANSACTION_TYPE : ");
				}
			}

		}

		return preferencesValueDTO;
	}

	public List<PreferencesDTO> getPreferencesListFromPrefrenceValues(
			List<PreferencesDTO> preferencesList,
			PreferencesValueDTO preferencesValueDTO) {
		List<PreferencesDTO> preferencesListTemp = new ArrayList<PreferencesDTO>();
		for (PreferencesDTO dto : preferencesList) {

			if (dto.getPreferencePrefix().equals("GENERATE_PROPOSAL_PDF")) {
				logger.debug("Proposal PDF enable : "
						+ preferencesValueDTO.getEnableProposalPDF());
				dto.setPreferenceEnable(preferencesValueDTO
						.getEnableProposalPDF());
			}

			if (dto.getPreferencePrefix().equals("PROPOSAL_REMIND_TIMES")) {
				dto.setPreferenceValue(String.valueOf(preferencesValueDTO
						.getProposalRemindTimes()));
			}
			if (dto.getPreferencePrefix().equals("PRPOSAL_REMIND_BETWEEN_DAYS")) {
				dto.setPreferenceValue(String.valueOf(preferencesValueDTO
						.getPrposalRemindBetweenDays()));
			}

			if (dto.getPreferencePrefix().equals("PROPOSAL_REMIND_MAIL_TIME")) {

				SimpleDateFormat formatter = new SimpleDateFormat(
						dateTimeFormatSQL);

				try {
					Date valueDate = getTimeFromTimeZoneToTimezone(
							preferencesValueDTO.getProposalRemindMailTime(),
							UserUtil.getUserTimezone(false),
							getServerTimeZone());

					while (valueDate.compareTo(new Date()) < 0) {
						valueDate = addDate(valueDate, 1);
					}

					dto.setPreferenceValue(formatter.format(valueDate.getTime()));

					// dto.setPreferenceValue(formatter.format(preferencesValueDTO
					// .getProposalRemindMailTime()));
				} catch (Exception e) {
					logger.error("Error in assigning preferences values", e);
				}
			}

			if (dto.getPreferencePrefix().equals("COMPANY_EMAIL_PREFIX")) {
				dto.setPreferenceValue(String.valueOf(preferencesValueDTO
						.getCompanyEmailPrefix()));
			}

			if (dto.getPreferencePrefix().equals("PROPOSAL_REMIND_TIMES")
					|| dto.getPreferencePrefix().equals(
							"PRPOSAL_REMIND_BETWEEN_DAYS")
					|| dto.getPreferencePrefix().equals(
							"PROPOSAL_REMIND_MAIL_TIME")) {
				dto.setPreferenceEnable(preferencesValueDTO
						.getEnablePrposalRemind());
			}

			if (dto.getPreferencePrefix().equals(
					"APPROVED_PROPOSAL_FOREMAN_INCENTIVE")) {
				dto.setPreferenceValue(String.valueOf(preferencesValueDTO
						.getProposalForemanIncentive()));

				dto.setPreferenceIncentiveEnable(preferencesValueDTO
						.getEnableForemanIncentive());

				dto.setPreferenceIncentiveApproval(preferencesValueDTO
						.getForemanIncentiveApproval());

			}

			if (dto.getPreferencePrefix().equals(
					"AUTOMATIC_INVITE_CLIENT_EMAIL")) {

				dto.setPreferenceEnable(preferencesValueDTO
						.getInviteClientEmail());
			}

			//

			if (dto.getPreferencePrefix().equals("ENABLE_QUICKBOOKS")) {

				int value = setBooleanEmpty(preferencesValueDTO
						.getEnableQuickBooksRemind()) ? 1 : 0;

				dto.setPreferenceValue(String.valueOf(value));
			}

			if (dto.getPreferencePrefix().equals("QUICKBOOKS_TRANSACTION_TYPE")) {
				dto.setPreferenceValue(preferencesValueDTO
						.getQuickBooksTransactionType());
			}
			if (dto.getPreferencePrefix().equals(
					"QUICKBOOKS_REMIND_BETWEEN_DAYS")) {
				dto.setPreferenceValue(String.valueOf(preferencesValueDTO
						.getQuickBooksRemindBetweenDays()));
			}

			if (dto.getPreferencePrefix().equals("QUICKBOOKS_MODULE")) {
				dto.setPreferenceValue(preferencesValueDTO
						.getQuickBooksModuleType());
			}

			if (dto.getPreferencePrefix().equals("QUICKBOOKS_REMIND_TIME")) {

				SimpleDateFormat formatter = new SimpleDateFormat(
						dateTimeFormatSQL);

				try {
					Date valueDate = getTimeFromTimeZoneToTimezone(
							preferencesValueDTO.getQuickBooksRemindTime(),
							UserUtil.getUserTimezone(false),
							getServerTimeZone());

					while (valueDate.compareTo(new Date()) < 0) {
						valueDate = addDate(valueDate, 1);
					}

					dto.setPreferenceValue(formatter.format(valueDate.getTime()));

					// dto.setPreferenceValue(formatter.format(preferencesValueDTO
					// .getProposalRemindMailTime()));
				} catch (Exception e) {
					logger.error("Error in assigning preferences values", e);
				}
			}

			//

			preferencesListTemp.add(dto);
		}

		return preferencesListTemp;
	}

	/**
	 * get Cappsure Admin Preferences List From Prefrence Bean
	 * 
	 * @param cappsurePreferencesList
	 *            - Cappsure Admin Preference List
	 * @param preferencesValueDTO
	 *            - Cappsure Admin Preference DTO
	 * @return - Cappsure Admin Preference List
	 */
	public List<CappsurePreferencesDTO> getCappsurePreferencesListFromPrefrenceValues(
			List<CappsurePreferencesDTO> cappsurePreferencesList,
			CappsurePreferencesValueDTO preferencesValueDTO) {

		List<CappsurePreferencesDTO> cappsurePreferencesListTemp = new ArrayList<CappsurePreferencesDTO>();
		for (CappsurePreferencesDTO cappsurePreferencesDTO : cappsurePreferencesList) {
			if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"ANDROID_APP_VERSION")) {
				cappsurePreferencesDTO.setPreferenceValue(String
						.valueOf(preferencesValueDTO.getAndroidAppVersion()));
			} else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"IOS_APP_VERSION")) {
				cappsurePreferencesDTO.setPreferenceValue(String
						.valueOf(preferencesValueDTO.getIosAppVersion()));
			} else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"ANDROID_BETA_VERSION")) {
				cappsurePreferencesDTO.setPreferenceValue(String
						.valueOf(preferencesValueDTO.getAndroidBetaVersion()));
			} else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"IOS_BETA_VERSION")) {
				cappsurePreferencesDTO.setPreferenceValue(String
						.valueOf(preferencesValueDTO.getIosBetaVersion()));
			} else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"SAFARI_VERSION")) {
				cappsurePreferencesDTO.setPreferenceValue(String
						.valueOf(preferencesValueDTO.getSafariVersion()));
			} else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"FIREFOX_VERSION")) {
				cappsurePreferencesDTO.setPreferenceValue(String
						.valueOf(preferencesValueDTO.getFirefoxVersion()));
			} else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"CHROME_VERSION")) {
				cappsurePreferencesDTO.setPreferenceValue(String
						.valueOf(preferencesValueDTO.getChromeVersion()));
			} else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"IE_VERSION")) {
				cappsurePreferencesDTO.setPreferenceValue(String
						.valueOf(preferencesValueDTO.getIeVersion()));
			} else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"MAILED_BY")) {
				cappsurePreferencesDTO.setPreferenceValue(String
						.valueOf(preferencesValueDTO.getMailedBy()));
			} else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"INVALID_EMAIL_PERIOD")) {
				cappsurePreferencesDTO.setPreferenceValue(String
						.valueOf(preferencesValueDTO
								.getInvalidEmailTimePeriod()));
			}

			else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"BETA_USER_COMPANY_ID")) {
				cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO
						.getBetaUserCompanyId());
			} else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"ANDROID_LOG")) {
				cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO
						.isAndroidLog() ? "true" : "false");
			} 
			//v1.19 Starts
			else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					CommonVariables.MIRAGE_BOUNDARY_METERS)) {
				cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO
						.getMirageBoundary());
			}
			//v1.19 Ends
			//v1.20 Starts
			else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					CommonVariables.CUSTOMIZED_BEACON)) {
				cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO
						.getCustomizedBeacon() ? "true" : "false");
			}
			//v1.20 Ends
			
			//v1.22 starts
		      else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
		          CommonVariables.BACKGROUND_LOCATION_SERVICE)) {
		        cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO
		            .getBackgroundLocationService() ? "true" : "false");
		      }
			//v1.22 ends
			// v1.23 starts
			else if (cappsurePreferencesDTO.getPreferencePrefix().equals(CommonVariables.GRACE_PERIOD)) {
				cappsurePreferencesDTO.setPreferenceValue(Integer.toString(preferencesValueDTO.getGracePeriod()));
			}
			// v1.23 ends
			//v1.25 Starts
				else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
						CommonVariables.CAPTCHA_V2_ALGORITHM)) {
					cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO
							.getCaptchaV2() ? "true" : "false");
				}
				/*else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
						CommonVariables.CAPTCHA_V3_ALGORITHM)) {
					cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO
							.getCaptchaV3() ? "true" : "false");
				}*/
			//v1.25 Ends
			else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					CommonVariables.USER_PER_MONTH_VALUE)) {
				cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO
						.getUserPerMonth());
			}else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
				CommonVariables.DEFAULT_PROMOTION_DAYS)) {
			cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO
					.getPromoDays());
		}else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"IOS_LOG")) {
				cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO
						.isIosLog() ? "true" : "false");
			} else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"ANDROID_LOG_CLEAR")) {
				cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO
						.isAndroidClearLog() ? "true" : "false");
			} else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"IOS_LOG_CLEAR")) {
				cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO
						.isIosClearLog() ? "true" : "false");
			}else if (cappsurePreferencesDTO.getPreferencePrefix().equals(
					"COMPANY_INVITE_LINK")) {
				if(preferencesValueDTO.getInviteType().equals("ALL")){
					cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO.getInviteType());
				}else if(preferencesValueDTO.getInviteType().equals("NONE")){
					cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO.getInviteType());
				}else if(preferencesValueDTO.getInviteType().equals("COMPANYID")){
					cappsurePreferencesDTO.setPreferenceValue(preferencesValueDTO.getInviteCompanyId());
				}				
			}

			cappsurePreferencesListTemp.add(cappsurePreferencesDTO);
		}

		return cappsurePreferencesListTemp;
	}

	public String mergeCompanyAddressForMail(CompanyDTO companyDTO) {
		String companyAddress = companyDTO.getCompanyBillingAddress() + "<br/>"
				+ companyDTO.getCompanyBillingCity() + ", "
				+ companyDTO.getCompanyBillingState() + ", "
				+ companyDTO.getCompanyBillingCountry() + " "
				+ companyDTO.getCompanyBillingZipCode();
		return companyAddress;
	}

	public String mergeCompanyAddressForPdf(CompanyDTO companyDTO) {
		String companyAddress = companyDTO.getCompanyBillingAddress() + "<br/>"
				+ companyDTO.getCompanyBillingCity() + ", "
				+ companyDTO.getCompanyBillingState() + ", "
				+ companyDTO.getCompanyBillingCountry() + " "
				+ companyDTO.getCompanyBillingZipCode();
		return companyAddress;
	}

	public String getSenderEmailPrefix(Integer companyId) {

		String senderEmailPrefix = "";
		try {

			PreferencesDAO dao = new PreferencesDAO();
			List<PreferencesDTO> preferencesList = dao
					.getPreferencesById(companyId);

			PreferencesValueDTO preferencesValueDTO = assignPrefrenceValue(preferencesList);

			if (preferencesValueDTO.getCompanyEmailPrefix().length() >= 1
					&& preferencesValueDTO.getCompanyEmailPrefix() != null) {
				senderEmailPrefix = preferencesValueDTO.getCompanyEmailPrefix()
						+ "-";
			}

		} catch (Exception e) {
			logger.error("Exception in get Sender Email Prefix ", e);
		}
		return senderEmailPrefix;

	}

	public Date getDateFromTime(Date mailTime) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, mailTime.getHours());
		cal.set(Calendar.MINUTE, mailTime.getMinutes());
		cal.set(Calendar.SECOND, 0);

		return cal.getTime();
	}

	public String getWeekDaysFromDate(Date date) {
		String day = "";
		try {
			SimpleDateFormat df = new SimpleDateFormat("EEE");
			day = df.format(date);
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return day;
	}

	public String setStringEmpty(String str, String defultValue) {
		if (str == null || str.trim().length() == 0) {
			str = defultValue;
		}
		return str.trim();
	}

	public String generateCompanyLogoForEmail(String companyLogo,
			String filePath, String fileName) {
		InputStream in = null;
		try {

			in = new ByteArrayInputStream(getCompnayLogoInBytes(companyLogo));
			BufferedImage bImageFromConvert = ImageIO.read(in);
			//MVIR Starts
			/*ImageIO.write(bImageFromConvert, "png", new File(filePath + "/"
					+ fileName));*/
			bImageFromConvert = getFormattedImage(bImageFromConvert);
			ImageIO.write(bImageFromConvert, "jpg", new File(filePath + "/"
					+ fileName));
			//MVIR Ends

		} catch (Exception e) {
			logger.error("Exception  in generate Company Logo For Email ", e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
		return filePath + "/" + fileName;
	}

	public String generateCompanyLogoURLByS3Bucket(String companyLogo,
			Integer companyId, AmazonS3 s3client, String fileSourcePath) {
		String s3BucketCompanyLogoURL = "";
		try {
			if (s3client == null) {
				s3client = getAmazonS3BucketAuthentication();
			}

			String generateCompanyLogoFile = generateCompanyLogoForEmail(
					companyLogo, fileSourcePath, companyId + ".png");

			File file = new File(generateCompanyLogoFile);

			s3BucketCompanyLogoURL = uploadFileAmazonS3Bucket(
					s3client,
					generateCompanyLogoFile,
					MessageLoader.getInstance().getMessageStatement(
							"MSG_S3_UPLOAD_COMPANY_LOGO_FOLDER")
							+ "/" + file.getName());

			if (file != null) {
				file.delete();
			}

		} catch (Exception e) {
			logger.error("Exception in generate Company Logo URL By S3Bucket ",
					e);
		}
		return s3BucketCompanyLogoURL;
	}

	/**
	 * Get amazon s3 bucket authentication from AWSCredentials.
	 * 
	 * @return s3client
	 */
	public AmazonS3 getAmazonS3BucketAuthentication() {
		AmazonS3 s3client = null;
		try {
			AWSCredentials credentials = new BasicAWSCredentials(MessageLoader
					.getInstance().getMessageStatement("MSG_S3_ACCESS_KEY"),
					MessageLoader.getInstance().getMessageStatement(
							"MSG_S3_SECRET_KEY"));
			s3client = new AmazonS3Client(credentials);

		} catch (Exception e) {
			logger.error("Exception in get amazon s3 bucket authentication ", e);
		}
		return s3client;
	}

	/**
	 * Upload file to amazon s3 bucket.
	 * 
	 * @param s3client
	 * @param attachFilePathName
	 * @param bucketFolderNameAndAttchFileName
	 * @return uploaded bucket URL.
	 */
	public String uploadFileAmazonS3Bucket(AmazonS3 s3client,
			String attachFilePathName, String bucketFolderNameAndAttchFileName) {
		String bucketURL = "";
		try {
			String bucketName = MessageLoader.getInstance()
					.getMessageStatement("MSG_S3_BUCKET_NAME");
			s3client.putObject(new PutObjectRequest(bucketName,
					bucketFolderNameAndAttchFileName, new File(
							attachFilePathName))
					.withCannedAcl(CannedAccessControlList.Private));
			// bucketURL = ((AmazonS3Client)
			// s3client).getResourceUrl(bucketName,
			// bucketFolderNameAndAttchFileName);

			bucketURL = ((AmazonS3Client) s3client).getResourceUrl(bucketName,
					bucketFolderNameAndAttchFileName);
			GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
					bucketName, bucketFolderNameAndAttchFileName);
			generatePresignedUrlRequest.setMethod(HttpMethod.GET);
			generatePresignedUrlRequest
					.setExpiration(getNextYearFromCurrentDate());
			URL url = s3client
					.generatePresignedUrl(generatePresignedUrlRequest);
			bucketURL = url.toString();

			java.util.logging.Logger.getLogger("org.apache.http").setLevel(
					java.util.logging.Level.INFO);
		} catch (Exception e) {
			logger.error("Exception in upload file amazon s3 bucket ", e);
		}
		return bucketURL;
	}

	/**
	 * Get amazon s3 bucket pre-sign URL.
	 * 
	 * @param s3client
	 * @param folderNameAndFileName
	 * @return s3BucketImageURL
	 */
	public String getS3BucketPreSignedURLByFolderNameAndFileName(
			AmazonS3 s3client, String folderNameAndFileName) {
		String s3BucketImageURL = "";
		try {
			String bucketName = MessageLoader.getInstance()
					.getMessageStatement("MSG_S3_BUCKET_NAME");

			s3BucketImageURL = ((AmazonS3Client) s3client).getResourceUrl(
					bucketName, folderNameAndFileName);
			GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
					bucketName, folderNameAndFileName);
			generatePresignedUrlRequest.setMethod(HttpMethod.GET);
			generatePresignedUrlRequest
					.setExpiration(getNextYearFromCurrentDate());
			URL url = s3client
					.generatePresignedUrl(generatePresignedUrlRequest);
			s3BucketImageURL = url.toString();

			java.util.logging.Logger.getLogger("org.apache.http").setLevel(
					java.util.logging.Level.INFO);
		} catch (Exception e) {
			logger.error(
					"Exception in get S3 Bucket PreSigned URL By Folder Name And FileName ",
					e);
		}
		return s3BucketImageURL;
	}

	/**
	 * Upload image to amazon s3 bucket by byte array images. Byte array image
	 * convert to image after store to s3.
	 * 
	 * @param imageBytearray
	 * @param s3client
	 * @param attachFilePathName
	 * @param bucketFolderNameAndAttchFileName
	 * @param destinationImageType
	 * @return uploaded file s3 url.
	 */
	public String uploadFileAmazonS3Bucket(byte[] imageBytearray,
			AmazonS3 s3client, String attachFilePathName,
			String bucketFolderNameAndAttchFileName, String destinationImageType) {
		String bucketURL = "";
		InputStream is = null;
		try {

			String bucketName = MessageLoader.getInstance()
					.getMessageStatement("MSG_S3_BUCKET_NAME");

			BufferedImage bufferedImage = ImageIO
					.read(new ByteArrayInputStream(imageBytearray));
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			//MVIR Starts
			//ImageIO.write(bufferedImage, "png", os);
			bufferedImage = getFormattedImage(bufferedImage);
			ImageIO.write(bufferedImage, "jpg", os);
			//MVIR Ends

			byte[] buffer = os.toByteArray();
			is = new ByteArrayInputStream(buffer);
			ObjectMetadata meta = new ObjectMetadata();
			meta.setContentLength(buffer.length);
			meta.setContentType("image/" + destinationImageType);
			// meta.setCacheControl("public,max-age=600");
			s3client.putObject(new PutObjectRequest(bucketName,
					bucketFolderNameAndAttchFileName, is, meta)
					.withCannedAcl(CannedAccessControlList.Private));

			bucketURL = ((AmazonS3Client) s3client).getResourceUrl(bucketName,
					bucketFolderNameAndAttchFileName);

			GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
					bucketName, bucketFolderNameAndAttchFileName);
			generatePresignedUrlRequest.setMethod(HttpMethod.GET);

			generatePresignedUrlRequest
					.setExpiration(getNextYearFromCurrentDate());

			URL url = s3client
					.generatePresignedUrl(generatePresignedUrlRequest);
			bucketURL = url.toString();

			java.util.logging.Logger.getLogger("org.apache.http").setLevel(
					java.util.logging.Level.INFO);
		} catch (Exception e) {
			logger.error("Exception in upload file amazon s3 bucket ", e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}

			}
		}
		return bucketURL;

	}

	/**
	 * Upload image to amazon s3 bucket by byte array images with status. Byte
	 * array image convert to image after store to s3.
	 * 
	 * @param imageBytearray
	 * @param s3client
	 * @param attachFilePathName
	 * @param bucketFolderNameAndAttchFileName
	 * @param destinationImageType
	 * @return uploaded file s3 url.
	 */
	public boolean uploadFileAmazonS3BucketWithResult(byte[] imageBytearray,
			AmazonS3 s3client, String attachFilePathName,
			String bucketFolderNameAndAttchFileName, String destinationImageType) {
		InputStream is = null;
		try {

			String bucketName = MessageLoader.getInstance()
					.getMessageStatement("MSG_S3_BUCKET_NAME");

			BufferedImage bufferedImage = ImageIO
					.read(new ByteArrayInputStream(imageBytearray));
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			//MVIR Starts
			//ImageIO.write(bufferedImage, destinationImageType, os);
			bufferedImage = getFormattedImage(bufferedImage);
			ImageIO.write(bufferedImage, "jpg", os);
			//MVIR Ends

			byte[] buffer = os.toByteArray();
			is = new ByteArrayInputStream(buffer);
			ObjectMetadata meta = new ObjectMetadata();
			meta.setContentLength(buffer.length);
			meta.setContentType("image/" + destinationImageType);
			s3client.putObject(new PutObjectRequest(bucketName,
					bucketFolderNameAndAttchFileName, is, meta)
					.withCannedAcl(CannedAccessControlList.Private));

			java.util.logging.Logger.getLogger("org.apache.http").setLevel(
					java.util.logging.Level.INFO);
		} catch (Exception e) {
			logger.error("Exception in upload file amazon s3 bucket ", e);
			return false;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}

			}
		}
		return true;

	}

	/**
	 * Upload image to amazon s3 bucket byBufferedImage.
	 * 
	 * @param imageBytearray
	 * @param s3client
	 * @param attachFilePathName
	 * @param bucketFolderNameAndAttchFileName
	 * @param destinationImageType
	 * @return status.
	 */
	public boolean uploadFileAmazonS3BucketWithBufferedImage(
			BufferedImage buffImage, AmazonS3 s3client,
			String attachFilePathName, String bucketFolderNameAndAttchFileName,
			String destinationImageType) {
		InputStream is = null;
		try {
			if (buffImage != null) {
				String bucketName = MessageLoader.getInstance()
						.getMessageStatement("MSG_S3_BUCKET_NAME");

				ByteArrayOutputStream os = new ByteArrayOutputStream();
				//v1.1 Starts
				//ImageIO.write(buffImage, destinationImageType, os);
				buffImage = getFormattedImage(buffImage);
				ImageIO.write(buffImage, "jpg", os);
				//v1.1 Ends

				byte[] buffer = os.toByteArray();
				is = new ByteArrayInputStream(buffer);
				ObjectMetadata meta = new ObjectMetadata();
				meta.setContentLength(buffer.length);
				meta.setContentType("image/" + destinationImageType);
				s3client.putObject(new PutObjectRequest(bucketName,
						bucketFolderNameAndAttchFileName, is, meta)
						.withCannedAcl(CannedAccessControlList.Private));

				java.util.logging.Logger.getLogger("org.apache.http").setLevel(
						java.util.logging.Level.INFO);
			}
			// String bucketURL = ((AmazonS3Client) s3client).getResourceUrl(
			// bucketName, bucketFolderNameAndAttchFileName);

		} catch (Exception e) {
			logger.error("Exception in upload file amazon s3 bucket ", e);
			return false;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}

			}
		}
		return true;

	}

	// /**
	// * Upload thumbnail images to S3 bucket
	// *
	// * @param imageBytearray
	// * @param s3client
	// * @param attachFilePathName
	// * @param bucketFolderNameAndAttchFileName
	// * @param destinationImageType
	// * @return
	// */
	// public boolean uploadThumbnailFileAmazonS3BucketWithResult(int a,
	// BufferedImage thumbnailImage, AmazonS3 s3client,
	// String attachFilePathName, String bucketFolderNameAndAttchFileName,
	// String destinationImageType) {
	// InputStream is = null;
	// try {
	//
	// String bucketName = MessageLoader.getInstance()
	// .getMessageStatement("MSG_S3_BUCKET_NAME");
	//
	// ByteArrayOutputStream os = new ByteArrayOutputStream();
	// ImageIO.write(thumbnailImage, destinationImageType, os);
	//
	// byte[] buffer = os.toByteArray();
	// is = new ByteArrayInputStream(buffer);
	// ObjectMetadata meta = new ObjectMetadata();
	// meta.setContentLength(buffer.length);
	// meta.setContentType("image/" + destinationImageType);
	// s3client.putObject(new PutObjectRequest(bucketName,
	// bucketFolderNameAndAttchFileName, is, meta)
	// .withCannedAcl(CannedAccessControlList.Private));
	//
	// java.util.logging.Logger.getLogger("org.apache.http").setLevel(
	// java.util.logging.Level.INFO);
	// } catch (Exception e) {
	// logger.error("Exception in upload Thumbnail File Amazon S3Bucket ",
	// e);
	// return false;
	// } finally {
	// if (is != null) {
	// try {
	// is.close();
	// } catch (IOException e) {
	// }
	//
	// }
	// }
	// return true;
	//
	// }
	/**
	 * Upload file to amazon s3 bucket.
	 * 
	 * @param s3client
	 * @param attachFilePathName
	 * @param bucketFolderNameAndAttchFileName
	 * @return
	 */
	public boolean uploadFileAmazonS3BucketWithResult(AmazonS3 s3client,
			String attachFilePathName, String bucketFolderNameAndAttchFileName) {

		try {
			String bucketName = MessageLoader.getInstance()
					.getMessageStatement("MSG_S3_BUCKET_NAME");
			s3client.putObject(new PutObjectRequest(bucketName,
					bucketFolderNameAndAttchFileName, new File(
							attachFilePathName))
					.withCannedAcl(CannedAccessControlList.Private));

			java.util.logging.Logger.getLogger("org.apache.http").setLevel(
					java.util.logging.Level.INFO);
		} catch (Exception e) {
			logger.error("Exception in upload file amazon s3 bucket ", e);
			return false;
		}
		return true;
	}

	public String uploadFileAmazonS3Bucket(byte[] imageBytearray,
			AmazonS3 s3client, String attachFilePathName,
			String bucketFolderNameAndAttchFileName) {
		return uploadFileAmazonS3Bucket(imageBytearray, s3client,
				attachFilePathName, bucketFolderNameAndAttchFileName, "png");
	}

	public String setGoogleAnalyticsTrackingScript(Integer companyId,
			String userRole) {
		String GaTrackingScript = "";
		try {
			GaTrackingScript = MessageLoader.getInstance().getMessageStatement(
					"MSG_GA_TRACK_SCRIPT_LOGGED");

			GaTrackingScript = GaTrackingScript.replace("TRACK-ID",
					getTrackingCompanyGAId(companyId));
			GaTrackingScript = GaTrackingScript.replace("USER-ROLE", userRole);

		} catch (Exception e) {
		}
		return GaTrackingScript;
	}

	/**
	 * Get google analytics track code. Get the track code from messages
	 * properties file. If not available in property file means default assign
	 * the company id instead of track id serial number, but that track code
	 * also mapped in property file means add "A" in last character.
	 * 
	 * @param companyId
	 * @return
	 */
	public String getTrackingCompanyGAId(Integer companyId) {
		String trackingGAId = MessageLoader.getInstance().getMessageStatement(
				"MSG_GA_TRACK_ID-DUMMY");
		if (companyId > 0) {
			try {
				trackingGAId = MessageLoader.getInstance().getMessageStatement(
						"MSG_GA_TRACK_ID-" + companyId);

				if (trackingGAId == null) {
					trackingGAId = MessageLoader.getInstance()
							.getMessageStatement("MSG_GA_DEFAULT_TRACK_ID")
							+ companyId;

					if (MessageLoader.getInstance().getMessageStatement(
							trackingGAId) != null
							&& MessageLoader.getInstance()
									.getMessageStatement(trackingGAId).trim()
									.length() > 2) {

						trackingGAId = MessageLoader.getInstance()
								.getMessageStatement("MSG_GA_DEFAULT_TRACK_ID")
								+ companyId + "A";
					}
				}
			} catch (Exception e) {
				logger.error(" Error in fetching Company " + companyId
						+ " GA id :", e);
			}
		}

		return trackingGAId;
	}

	public Date getNextYearFromCurrentDate() {
		Date nextYear = new Date();
		try {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, 365);
			nextYear = cal.getTime();
		} catch (Exception e) {
			logger.error("Exception in get next year from current date ", e);
		}
		return nextYear;
	}

	/**
	 * Convert image string to image byte array.
	 * 
	 * @param imageString
	 * @return byte array image.
	 */
	public byte[] convertImageStringToByteArray(String imageString) {
		ByteArrayOutputStream os = null;
		byte[] byteImage = null;
		try {
			imageString = imageString.substring(imageString.indexOf(",") + 1);
			BufferedImage buffImage = CommonDAO.decodeToImage(imageString);
			os = new ByteArrayOutputStream();
			//MVIR Starts
			//ImageIO.write(buffImage, "png", os);
			buffImage = getFormattedImage(buffImage);
			ImageIO.write(buffImage, "jpg", os);
			//MVIR Ends
			byteImage = os.toByteArray();
		} catch (Exception e) {
			logger.error("Exception in convert image String to ByteArray ", e);
		} finally {
			try {
				os.close();
			} catch (IOException e) {
			}
		}
		return byteImage;
	}

	/**
	 * Get category count by company id
	 * 
	 * @param companyId
	 * @return category company count
	 */
	public Integer getCategoryCountByCompanyId(int companyId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		Integer categoryCompanyCount = 0;
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "select count(icId) from ff_inventory_category where icRowStatus is null and icComId=?";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					categoryCompanyCount = resInfo.getInt(1);
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return categoryCompanyCount;
	}

	/**
	 * @param map
	 * @param value
	 * @return
	 */
	public static Integer getKeyByValue(Map<Integer, String> map, String value) {
		for (Entry<Integer, String> entry : map.entrySet()) {
			if (Objects.equals(value, entry.getValue())) {
				return entry.getKey();
			}
		}
		return 0;
	}

	/**
	 * Invalidating cache
	 */
	public void invalidateInventoryResponseCache(String inventoryType,
			Integer companyId) {

		logger.debug("Invalidate ==  : " + " inv Type : " + inventoryType
				+ " comp id : " + companyId + " user type : "
				+ UserType.FOREMAN.getUserDesc());

		CacheUtil cacheInstance = CacheUtil.getInstance();
		cacheInstance.invalidate(new InventoryCacheKey(
				InventoryClassification.GROUP, inventoryType, companyId,
				UserType.FOREMAN.getUserDesc()));
		cacheInstance.invalidate(new InventoryCacheKey(
				InventoryClassification.GROUP, inventoryType, companyId,
				UserType.CLIENT.getUserDesc()));

		cacheInstance = CacheUtil.getInstance();
		cacheInstance.invalidate(new InventoryCacheKey(
				InventoryClassification.CATEGORY, inventoryType, companyId,
				UserType.FOREMAN.getUserDesc()));
		cacheInstance.invalidate(new InventoryCacheKey(
				InventoryClassification.CATEGORY, inventoryType, companyId,
				UserType.CLIENT.getUserDesc()));

		cacheInstance = CacheUtil.getInstance();
		cacheInstance.invalidate(new InventoryCacheKey(
				InventoryClassification.ITEMS, inventoryType, companyId,
				UserType.FOREMAN.getUserDesc()));
		cacheInstance.invalidate(new InventoryCacheKey(
				InventoryClassification.ITEMS, inventoryType, companyId,
				UserType.CLIENT.getUserDesc()));

	}

	/**
	 * Pass condition to prepared statement
	 * 
	 * @param preparedStatement
	 * @param values
	 * @throws SQLException
	 */
	public static void setValues(PreparedStatement preparedStatement,
			List<Object> qryParameterList) {
		for (int i = 0; i < qryParameterList.size(); i++) {
			try {
				preparedStatement.setObject(i + 1, qryParameterList.get(i));
			} catch (Throwable e) {
				logger.error("SQL Exception in set query parameters ", e);
			}
		}
	}

	/**
	 * Assign sorting column based on column name, sort field, sort order
	 * 
	 * @param columnName
	 * @param sortField
	 * @param sortOrder
	 * @return sortMeta
	 */
	public SortMeta defaultSortMetaField(UIComponent columnName,
			String sortField, SortOrder sortOrder) {
		SortMeta sortMeta = new SortMeta();
		sortMeta.setSortBy((UIColumn) columnName);
		sortMeta.setSortField(sortField);
		//v1.3 Starts
		//sortMeta.setSortOrder(SortOrder.ASCENDING);
		sortMeta.setSortOrder(sortOrder);
		//v1.3 Ends
		return sortMeta;
	}

	/**
	 * Address split up ( *** Need to be change this functionality ***)
	 * 
	 * @param parseText
	 * @return
	 */
	public PlaceDTO addressParser(String parseText) {

		PlaceDTO placeDTO = new PlaceDTO(0);
		String streetAddress = "";
		try {
			String[] splitAddress = parseText.trim().split(",");

			if (splitAddress[splitAddress.length - 1] != null) {
				placeDTO.setPlaceCountry(splitAddress[splitAddress.length - 1]
						.trim());
			}

			if (splitAddress[splitAddress.length - 2] != null) {
				placeDTO.setPlaceState(splitAddress[splitAddress.length - 2]
						.trim());
			}

			if (splitAddress[splitAddress.length - 3] != null) {
				placeDTO.setPlaceCity(splitAddress[splitAddress.length - 3]
						.trim());
			}

			for (int i = 0; i < splitAddress.length - 3; i++) {
				streetAddress = streetAddress + splitAddress[i]
						+ (i + 1 < splitAddress.length - 3 ? ", " : "");
			}
			placeDTO.setPlaceStreetAddress(streetAddress);

		} catch (Exception e) {
			logger.error("Exception in place parser ", e);
		}
		return placeDTO;
	}

	public List<String> completeAutoCompleteAddress(String searchAddress) {
		PlacesServiceDAO placesServiceDAO = new PlacesServiceDAO();
		List<String> address = new ArrayList<String>();
		ArrayList<PlaceDTO> placeDTOList = placesServiceDAO
				.autocomplete(searchAddress);
		for (PlaceDTO placeDTO : placeDTOList) {
			address.add(placeDTO.getPlaceName());
		}
		return address;
	}

	/**
	 * Comparison of two versions (Android App,IOS App,Browser versions)
	 * 
	 * @param browserVersion
	 * @param minimumVersion
	 * @return
	 */
	public int compareStringVersion(String browserVersion, String minimumVersion) {
		String[] vals1 = browserVersion.split("\\.");
		String[] vals2 = minimumVersion.split("\\.");
		int i = 0;
		// set index to first non-equal ordinal or length of shortest
		// version string
		while (i < vals1.length && i < vals2.length
				&& vals1[i].equals(vals2[i])) {
			i++;
		}
		int diff = 0;
		// compare first non-equal ordinal number
		if (i < vals1.length && i < vals2.length) {
			diff = Integer.signum(Integer.valueOf(vals1[i]).compareTo(
					Integer.valueOf(vals2[i])));
		} else {
			diff = Integer.signum(vals1.length - vals2.length);
		}
		return diff;
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	public static ProposalApprovalStatus getProposalApprovalStatusByValue(
			int value) {
		for (ProposalApprovalStatus status : ProposalApprovalStatus.values()) {
			if (value == status.getValue()) {
				return status;
			}
		}
		return null;
	}

	/**
	 * Get proposal approval status by label.
	 * 
	 * @param label
	 * @return
	 */
	public static ProposalApprovalStatus getProposalApprovalStatusByLabel(
			String label) {
		for (ProposalApprovalStatus status : ProposalApprovalStatus.values()) {
			if (label.equalsIgnoreCase(status.getLabel())) {
				return status;
			}
		}
		return null;
	}

	/**
	 * Get payment by value.
	 * 
	 * @param value
	 * @return
	 */
	public static PaymentTypeEnum getPaymentTypeByValue(int value) {
		for (PaymentTypeEnum status : PaymentTypeEnum.values()) {
			if (value == status.getValue()) {
				return status;
			}
		}
		return null;
	}

	/**
	 * Get payment by label.
	 * 
	 * @param label
	 * @return
	 */
	public static PaymentTypeEnum getPaymentTypeByLabel(String label) {
		for (PaymentTypeEnum status : PaymentTypeEnum.values()) {
			if (label.equalsIgnoreCase(status.getLabel())) {
				return status;
			}
		}
		return null;
	}

	/**
	 * Convert byte array images to buffered image after convert buffered image
	 * to thumbnail buffered image . Thumbnail converter using google
	 * thumbnailator. Thumbnail image size at 200 * 200.
	 * 
	 * @param image
	 * @return thumbnail buffered image.
	 */
	//MVIR Starts
	//public static BufferedImage generateThumbnailImageByByteArray(byte[] image) {
	public static BufferedImage generateThumbnailImageByByteArray(byte[] image) {
		return generateThumbnailImageByByteArray(image, false);
	}
	
	public static BufferedImage generateThumbnailImageByByteArray(byte[] image, boolean isAPIRequest) {
	//MVIR Ends
		BufferedImage originalImage = null;
		BufferedImage thumbnailImage = null;
		try {
			originalImage = ImageIO.read(new ByteArrayInputStream(image));
			//v1.1 Starts
			/*thumbnailImage = Thumbnails.of(originalImage)
					.size(THUMBNAIL_IMAGE_WIDTH, THUMBNAIL_IMAGE_HEIGHT)
					.asBufferedImage();*/
			thumbnailImage = isAPIRequest?Thumbnails.of(originalImage).scale(1).asBufferedImage():Thumbnails.of(originalImage).scale(1).outputQuality(0.5).asBufferedImage();
			//v1.1 Ends
		} catch (Exception e) {
			logger.error("Exception in generateThumbnailImage ", e);
		}

		return thumbnailImage;
	}

	/**
	 * Convert buffered image to thumbnail buffered image . Thumbnail converter
	 * using google thumbnailator. Thumbnail image size at 200 * 200.
	 * 
	 * @param image
	 * @return thumbnail buffered image.
	 */
	//MVIR Starts
	/*public static BufferedImage generateThumbnailImageByBufferedImage(
			BufferedImage image) {*/
	public static BufferedImage generateThumbnailImageByBufferedImage(
			BufferedImage image) {
		return generateThumbnailImageByBufferedImage(image, false);
	}
	public static BufferedImage generateThumbnailImageByBufferedImage(
			BufferedImage image, boolean isAPIRequest) {
	//MVIR Ends
		BufferedImage thumbnailImage = null;
		try {
			if (image != null) {
				
				// for testing - need to change after test
//				thumbnailImage=image;
//				logger.info("Bufferd image size :"+thumbnailImage.getWidth()+" >> "+thumbnailImage.getHeight());
				//v1.1 Starts
				/*thumbnailImage = Thumbnails.of(image)
						.size(THUMBNAIL_IMAGE_WIDTH, THUMBNAIL_IMAGE_HEIGHT)
						.asBufferedImage();*/
				thumbnailImage = isAPIRequest?Thumbnails.of(image).scale(1).asBufferedImage():Thumbnails.of(image).scale(1).outputQuality(0.5).asBufferedImage();
				//v1.1 Ends
			}
		} catch (Exception e) {
			logger.error("Exception in generateThumbnailImage ", e);
		}
		return thumbnailImage;
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	/**
	 * Get Company Thumbnail logo by company id.
	 * 
	 * @param companyId
	 * @return image bytes.
	 */
	public byte[] getCompanyThumbnailImageBytes(Integer companyId) {
		byte[] sImage = null;
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		do {
			retry = false;
			try {
				logger.info("com id : " + companyId);
				con = db2Connector.getConnection(true);
				String selectStatement = "Select comThumbnailLogo from ff_company_management where comRowStatus is null and comId=?";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				if (resInfo.next()) {
					sImage = resInfo.getBytes(1);
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return sImage;
	}

	/**
	 * Get Company logo by company id.
	 * 
	 * @param companyId
	 * @return image bytes.
	 */
	public byte[] getCompanyImageBytes(Integer companyId) {
		byte[] sImage = null;
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		do {
			retry = false;
			try {

				con = db2Connector.getConnection(true);
				String selectStatement = "Select comLogo from ff_company_management where comRowStatus is null and comId=?";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				if (resInfo.next()) {
					sImage = resInfo.getBytes(1);
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return sImage;
	}

	/**
	 * To generate company logo for excel report.
	 * 
	 * @param logo
	 * @param wb
	 * @param sheet
	 */
	public void companyLogoHeader(byte[] headerImage, Workbook wb, Sheet sheet) {
		try {
			int imageHeight = 0;
			// Add a picture to the workbook
			if (headerImage != null) {
				BufferedImage bufferedImage = convertByteToImage(headerImage);
				if (bufferedImage != null) {
					imageHeight = bufferedImage.getHeight();
				}
				int pictureIdx = wb.addPicture(headerImage,
						Workbook.PICTURE_TYPE_PNG);

				CreationHelper helper = wb.getCreationHelper();
				Drawing drawing = sheet.createDrawingPatriarch();
				ClientAnchor anchor = helper.createClientAnchor();
				anchor.setCol1(0);
				anchor.setRow1(0);
				anchor.setCol2(0);
				anchor.setRow2(0);

				anchor.setDx1(1);
				anchor.setDx2(1);
				anchor.setDy1(1);
				anchor.setDy2(1);
				anchor.setAnchorType(1);

				if (imageHeight >= 40) {
					imageHeight = imageHeight - 20;
				}

				Row row = sheet.createRow(0);
				row.setHeightInPoints(imageHeight);
				sheet.addMergedRegion(new CellRangeAddress(0, // first row
																// (0-based)
						0, // last row (0-based)
						0, // first column (0-based)
						50 // last column (0-based)
				));
				sheet.setColumnWidth(0, 20000);

				Picture pict = drawing.createPicture(anchor, pictureIdx);
				pict.resize();
				//
				// if (imageHeight <= 200) {
				// pict.resize(0.44);
				// } else if (imageHeight >= 201 && imageHeight <= 400) {
				// pict.resize(0.2);
				// } else if (imageHeight >= 401 && imageHeight <= 600) {
				// pict.resize(0.1);
				// } else if (imageHeight >= 601 && imageHeight <= 800) {
				// pict.resize(0.90);
				// }

			}
		} catch (Exception e) {
			logger.error("Exceptoin in companyLogoHeader in excel"
					+ e.toString());
		}
	}

	/**
	 * Set company logo footer in excel
	 * 
	 * @param wb
	 * @param sheet
	 * @param rowCount
	 * @param colCount
	 */
	public void companyLogoFooter(Workbook wb, Sheet sheet, int rowCount,
			int colCount) {
		// FacesContext ctx = FacesContext.getCurrentInstance();
		// String path = ctx.getExternalContext().getRealPath("/");
		String path = CommonDAO.getSourceFilePath();
		String logopath1 = path
				+ "/resources/assets/img/cappsureit_logo_powered-by-mail.png";

		InputStream inputStream;
		byte[] capsureImage = null;
		try {
			inputStream = new FileInputStream(logopath1);
			try {
				capsureImage = IOUtils.toByteArray(inputStream);

			} catch (IOException e) {

			}
		} catch (FileNotFoundException e) {

		}

		// Adds a picture to the workbook
		int capsureIdx = wb.addPicture(capsureImage, Workbook.PICTURE_TYPE_PNG);
		CreationHelper helperImage = wb.getCreationHelper();
		Drawing drawingImage = sheet.createDrawingPatriarch();
		ClientAnchor capsueanchor = helperImage.createClientAnchor();

		colCount = colCount / 2;

		capsueanchor.setCol1(colCount);
		capsueanchor.setRow1(rowCount + 2);
		Row r = sheet.createRow(rowCount + 2);
		r.setHeight((short) 750);

		sheet.addMergedRegion(new CellRangeAddress(rowCount + 2, // first row
				// (0-based)
				rowCount + 2, // last row (0-based)
				0, // first column (0-based)
				50 // last column (0-based)
		));
		sheet.setColumnWidth(colCount, 10000);

		Picture capsuePicture = drawingImage.createPicture(capsueanchor,
				capsureIdx);

		capsuePicture.resize(1);

	}

	/**
	 * Set Company logo in excel report.
	 * 
	 * @param logo
	 * @param wb
	 * @param sheet
	 */
	public void companyLogoHeaderExcel(String logo, Workbook wb, Sheet sheet) {
		try {
			byte[] headerImage = getCompnayLogoInBytes(logo);
			// int imageHeight = 0;
			// Adds a picture to the workbook
			if (headerImage != null) {
				// InputStream in = new ByteArrayInputStream(headerImage);
				// BufferedImage bufferedImage = ImageIO.read(in);
				// if (bufferedImage != null) {
				// imageHeight = bufferedImage.getHeight();
				// }

				int pictureIdx = wb.addPicture(headerImage,
						Workbook.PICTURE_TYPE_PNG);

				// Returns an object that handles instantiating concrete classes
				CreationHelper helper = wb.getCreationHelper();
				// Creates the top-level drawing patriarch.
				Drawing drawing = sheet.createDrawingPatriarch();

				// Create an anchor that is attached to the work sheet
				ClientAnchor anchor = helper.createClientAnchor();

				// create an anchor with upper left cell _and_ bottom right
				// cell
				anchor.setCol1(0); // Column A
				anchor.setRow1(0); // Row 0

				anchor.setCol2(1); // Column C
				anchor.setRow2(1); // Row 1

				// Creates a picture
				Picture pict = drawing.createPicture(anchor, pictureIdx);

				// Reset the image to the original size
				// pict.resize(); //don't do that. Let the anchor resize the
				// image!

				// Create the Cell B3
				Cell cell = sheet.createRow(0).createCell(0);

				// set width to n character widths = count characters * 256
				int widthUnits = 20 * 256;
				sheet.setColumnWidth(1, widthUnits);

				// set height to n points in twips = n * 20
				short heightUnits = 50 * 20;
				cell.getRow().setHeight(heightUnits);

				sheet.addMergedRegion(new CellRangeAddress(0, // first row
																// (0-based)
						0, // last row (0-based)
						0, // first column (0-based)
						50 // last column (0-based)
				));

				// int pictureIdx = wb.addPicture(headerImage,
				// Workbook.PICTURE_TYPE_PNG);
				//
				// CreationHelper helper = wb.getCreationHelper();
				// Drawing drawing = sheet.createDrawingPatriarch();
				// ClientAnchor anchor = helper.createClientAnchor();
				// anchor.setCol1(0);
				// anchor.setRow1(0);
				// anchor.setCol2(0);
				// anchor.setRow2(0);
				//
				// anchor.setDx1(1);
				// anchor.setDx2(1);
				// anchor.setDy1(1);
				// anchor.setDy2(1);
				// anchor.setAnchorType(1);
				//
				// Row row = sheet.createRow(0);
				// row.setHeight((short) 1000);
				// sheet.addMergedRegion(new CellRangeAddress(0, // first row
				// // (0-based)
				// 0, // last row (0-based)
				// 0, // first column (0-based)
				// 50 // last column (0-based)
				// ));
				// sheet.setColumnWidth(0, 20000);
				//
				// Picture pict = drawing.createPicture(anchor, pictureIdx);
				// if (imageHeight <= 200) {
				// pict.resize(0.44);
				// } else if (imageHeight >= 201 && imageHeight <= 400) {
				// pict.resize(0.2);
				// } else if (imageHeight >= 401 && imageHeight <= 600) {
				// pict.resize(0.1);
				// } else if (imageHeight >= 601 && imageHeight <= 800) {
				// pict.resize(0.90);
				// }

			}
		} catch (Exception e) {
			logger.error("Exceptoin in companyLogoHeader in excel", e);
		}
	}

	/**
	 * Validate user address format - city,state,country
	 * 
	 * @param placeDTO
	 * @return
	 */

	public int validateAddressCityStateCountry(PlaceDTO placeDTO) {
		StringBuilder errorMsgBuilder = new StringBuilder("Enter valid ");
		int ret = 0;
		try {

			if (placeDTO.getPlaceCity().trim().length() == 0) {
				ret++;
				errorMsgBuilder.append("city, ");
			}

			if (placeDTO.getPlaceState().trim().length() == 0) {
				ret++;
				errorMsgBuilder.append("state, ");
			}

			if (placeDTO.getPlaceCountry().trim().length() == 0) {
				ret++;
				errorMsgBuilder.append("country");
			}

		} catch (Exception e) {
			logger.error(
					"Exception in validate Company Billing Address City State Country ",
					e);
		}
		return ret;
	}

	/**
	 * 
	 * @param deviceID
	 * @param flag
	 * @return
	 */

	public void insertDeviceMailSent(String deviceID, Integer flag) {
		try {
			DBConnector db2Connector = DBConnector.getInstance();
			boolean retry;
			int numOfretry = 0;
			Connection con = null;
			PreparedStatement prepStmnt = null;
			ResultSet resInfo = null;
			do {
				retry = false;

				try {
					con = db2Connector.getConnection(true);
					String selectStatement = "Insert into ff_device (devDeviceID,devFlag) values (?,?) ";
					prepStmnt = con.prepareStatement(selectStatement);
					prepStmnt.setString(1, deviceID);
					prepStmnt.setInt(2, flag);
					prepStmnt.executeUpdate();
				} catch (SQLException scon) {
					logger.error("Select SQLException.." + scon + "  "
							+ numOfretry);
					if (numOfretry < 2) {
						numOfretry++;
						try {
							close(con);
						} catch (Exception e) {
						}

						con = db2Connector.getConnection(true);
						retry = true;
					} else {
						retry = false;
						logger.error("Select Exception :" + scon.getMessage());
					}
				} finally {
					close(resInfo);
					close(prepStmnt);
					close(con);
				}
			} while (retry);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

	}

	/**
	 * 
	 * @param deviceID
	 * @return
	 */
	public boolean getDeviceIDSentForAssignment(String deviceID) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select devDeviceID from ff_device where devDeviceID=? and devFlag=?";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setString(1, deviceID);
				prepStmnt.setInt(2, 1);
				resInfo = prepStmnt.executeQuery();
				if (resInfo.next()) {
					return true;
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException..", scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}

					con = db2Connector.getConnection(true);
				} else {
					logger.error("Select Exception :", scon);
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return false;
	}

	public Boolean deleteDevice(String deviceID) {
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		try {
			String selectStatement = "DELETE FROM ff_device where devDeviceID=?";
			prepStmnt = ConnectionHelper.getConnection().prepareStatement(
					selectStatement);
			prepStmnt.setString(1, deviceID);
			prepStmnt.executeUpdate();
			return true;
		} catch (SQLException scon) {
			logger.error("Delete SQLException..", scon);
		} finally {
			close(resInfo);
			close(prepStmnt);
		}
		return false;
	}

	/**
	 * Load proposal status
	 * 
	 * @param type
	 * @return
	 */
	public List<SelectItem> loadProposalMaintenanceStatusList(String type) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> proposalMaintenanceStatusList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select psId, psName from ff_proposal_maintenance_status where psType=? ";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setString(1, type);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					proposalMaintenanceStatusList.add(new SelectItem(""
							+ resInfo.getInt("psId"), getString(resInfo,
							"psName", "")));
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return proposalMaintenanceStatusList;
	}

	/**
	 * Load client proposal status
	 * 
	 * @param type
	 * @return
	 */
	public List<SelectItem> loadClientProposalMaintenanceStatusList(String type) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> proposalMaintenanceStatusList = new ArrayList<SelectItem>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select psId, psName from ff_proposal_maintenance_status where psType=? and psId not in (?,?) ";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setString(1, type);
				prepStmnt.setString(2,
						ProposalMaintenanceStatus.PRO_UN_SUBMITTED);
				prepStmnt.setString(3, ProposalMaintenanceStatus.PRO_PAID);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					proposalMaintenanceStatusList.add(new SelectItem(""
							+ resInfo.getInt("psId"), getString(resInfo,
							"psName", "")));
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return proposalMaintenanceStatusList;
	}

	/**
	 * Convert byte array image type to buffered image type
	 * 
	 * @param byteImage
	 * @return bufferedImage
	 */
	public BufferedImage convertByteToImage(byte[] byteImage) {
		BufferedImage bufferedImage = null;
		try {
			InputStream in = new ByteArrayInputStream(byteImage);
			bufferedImage = ImageIO.read(in);
		} catch (Exception e) {
			logger.error("Exception in convert byte to image format", e);
		}
		return bufferedImage;
	}

	/**
	 * Generate URL for google analytics event tracking.
	 * 
	 * @param eventCategory
	 * @param eventAction
	 * @param eventLabel
	 * @param userId
	 * @param companyId
	 * @return trackingurl
	 */
	public String generateGoogleAnalyticsTrackingURL(String eventCategory,
			String eventAction, String eventLabel, Integer userId,
			Integer companyId) {
		String trackingCode = "";
		try {

			String trackingContent = "https://www.google-analytics.com/collect?v=1&tid="
					+ getTrackingCompanyGAId(companyId)
					+ "&cid="
					+ userId
					+ "&t=event&dp=/"
					+ eventCategory
					+ "/&ec="
					+ eventCategory
					+ "&ea="
					+ eventAction
					+ "&el="
					+ eventLabel
					+ "&cs=notification&cm="
					+ eventCategory
					+ "&cn=click_event_tracking";

			URL url = new URL(trackingContent);
			URI uri = new URI(url.getProtocol(), url.getUserInfo(),
					url.getHost(), url.getPort(), url.getPath(),
					url.getQuery(), url.getRef());

			trackingCode = uri.toASCIIString();

		} catch (Exception e) {
			logger.error(
					"Exception in generate Google Analytics Tracking URL ", e);
		}
		return trackingCode;
	}

	/**
	 * Get PreferencesValueDTO from comapanyID
	 * 
	 * @param companyId
	 * @return
	 */
	public PreferencesValueDTO getPreferencesValueDTO(Integer companyId) {
		List<PreferencesDTO> listPreferencesDTO = new ArrayList<PreferencesDTO>();
		PreferencesValueDTO preferencesValueDTO = new PreferencesValueDTO(0);
		PreferencesDAO preferencesDAO = new PreferencesDAO();
		try {
			listPreferencesDTO = preferencesDAO.getPreferencesById(companyId);

			CommonDAO commonDAO = new CommonDAO();
			preferencesValueDTO = commonDAO
					.assignPrefrenceValueFromServlet(listPreferencesDTO);
		} catch (Exception e) {
			logger.error(
					"Error in assigning preferences values from data store", e);
		}
		return preferencesValueDTO;
	}

	/**
	 * Remove Special Character
	 * 
	 * @param Str
	 * @return
	 */
	public static String removeSpecialCharacter(String Str) {
		String retStr = "";
		try {
			retStr = Str.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬", "A")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¯Ã‚Â¿Ã‚Â½", "A")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡", "A")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢", "A")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾", "A")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦", "A")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ", "A")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡", "C")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€¹Ã¢â‚¬Â ", "E")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â°", "E")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€¦Ã‚Â ", "E")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹", "E")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€¦Ã¢â‚¬â„¢", "I")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¯Ã‚Â¿Ã‚Â½", "I")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€¦Ã‚Â½", "I")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¯Ã‚Â¿Ã‚Â½", "I")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¯Ã‚Â¿Ã‚Â½", "D")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“", "N")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢", "O")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ", "O")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã¯Â¿Â½", "O")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢", "O")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“", "O")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€¹Ã…â€œ", "O")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢", "U")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€¦Ã‚Â¡", "U")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Âº", "U")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€¦Ã¢â‚¬Å“", "U")
					.replace("ÃƒÆ’Ã†â€™ÃƒÂ¯Ã‚Â¿Ã‚Â½", "Y")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€¦Ã‚Â¾", "Y")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€¦Ã‚Â¸", "Z")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â ", "a")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡", "a")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢", "a")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â£", "a")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¤", "a")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¥", "a")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¦", "a")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â§", "c")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨", "e")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©", "e")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âª", "e")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â«", "e")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¬", "i")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­", "i")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â®", "i")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¯", "i")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â°", "o")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â±", "n")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â²", "o")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³", "o")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â´", "o")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âµ", "o")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¶", "o")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¸", "o")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¹", "u")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âº", "u")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â»", "u")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¼", "u")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â½", "y")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¾", "y")
					.replace("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¿", "y")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬", "A")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¯Ã‚Â¿Ã‚Â½", "a")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡", "A")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€ Ã¢â‚¬â„¢", "a")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾", "A")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦", "a")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ", "C")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡", "c")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€¹Ã¢â‚¬Â ", "C")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â°", "c")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€¦Ã‚Â ", "C")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹", "c")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€¦Ã¢â‚¬â„¢", "C")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¯Ã‚Â¿Ã‚Â½", "c")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€¦Ã‚Â½", "D")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¯Ã‚Â¿Ã‚Â½", "d")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¯Ã‚Â¿Ã‚Â½", "D")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“", "d")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢", "E")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ", "e")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“", "E")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬ï¿½", "e")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€¹Ã…â€œ", "E")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢", "e")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€¦Ã‚Â¡", "E")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‚Âº", "e")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€¦Ã¢â‚¬Å“", "G")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¯Ã‚Â¿Ã‚Â½", "g")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€¦Ã‚Â¾", "G")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€¦Ã‚Â¸", "g")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â ", "G")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¡", "g")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¢", "G")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¤", "H")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¥", "h")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¦", "H")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â§", "h")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¨", "I")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â©", "i")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Âª", "I")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â«", "i")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â®", "J")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¯", "j")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â´", "j")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Âµ", "j")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¶", "K")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â·", "k")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¸", "k")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¹", "L")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Âº", "l")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â»", "L")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¼", "l")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â½", "L")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¾", "l")
					.replace("ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚Â¿", "L")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬", "l")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¯Ã‚Â¿Ã‚Â½", "L")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡", "l")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€ Ã¢â‚¬â„¢", "N")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾", "n")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦", "N")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ", "n")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡", "N")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€¹Ã¢â‚¬Â ", "n")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â°", "n")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€¦Ã‚Â ", "N")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹", "n")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€¦Ã¢â‚¬â„¢", "O")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¯Ã‚Â¿Ã‚Â½", "o")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¯Ã‚Â¿Ã‚Â½", "O")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“", "o")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢", "Q")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ", "q")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã¯Â¿Â½", "R")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢", "r")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“", "R")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬ï¿½", "r")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€¹Ã…â€œ", "R")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢", "r")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€¦Ã‚Â¡", "S")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã‚Âº", "s")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€¦Ã¢â‚¬Å“", "S")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¯Ã‚Â¿Ã‚Â½", "s")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€¦Ã‚Â¾", "S")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€¦Ã‚Â¸", "s")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â ", "S")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡", "s")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¢", "T")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â£", "t")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¤", "T")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¥", "t")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¦", "T")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â§", "t")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¨", "U")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â©", "u")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Âª", "U")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â«", "u")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¬", "U")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â­", "u")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â®", "U")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¯", "u")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â°", "U")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â±", "u")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â²", "U")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â³", "u")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â´", "W")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Âµ", "w")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¶", "Y")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â·", "y")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¸", "y")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¹", "Z")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Âº", "z")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â»", "Z")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¼", "z")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â½", "Z")
					.replace("ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¾", "z")
					.replace("ÃƒÆ’Ã¢â‚¬Â Ãƒâ€šÃ‚Âµ", "Z")
					.replace("ÃƒÆ’Ã¢â‚¬Â¡Ãƒâ€šÃ‚Âµ", "g");
		} catch (Exception e) {
		}
		return retStr;
	}

	/**
	 * Converts the given <code>date</code> from the <code>fromTimeZone</code>
	 * to the <code>toTimeZone</code>. Since java.util.Date has does not really
	 * store time zome information, this actually converts the date to the date
	 * that it would be in the other time zone.
	 * 
	 * @param date
	 * @param fromTimeZone
	 * @param toTimeZone
	 * @return
	 */
	public static Date convertTimeZone(Date date, String strFromTimeZone,
			String strToTimeZone) {
		
		long fromTimeZoneOffset = 0;
		long toTimeZoneOffset = 0;
		
		try {
           
			strToTimeZone = StringUtils.isEmpty(strToTimeZone)? CommonDAO.getServerTimeZone():strToTimeZone;
			
			if(date == null || StringUtils.isEmpty(strToTimeZone)) {
				logger.info("Invalid date received to convert the date into user timezone date");
				return date;
			}
            
          
			
			TimeZone fromTimeZone = TimeZone.getTimeZone(strFromTimeZone);
			TimeZone toTimeZone = TimeZone.getTimeZone(strToTimeZone);

			fromTimeZoneOffset = getTimeZoneUTCAndDSTOffset(date, fromTimeZone);
			toTimeZoneOffset = getTimeZoneUTCAndDSTOffset(date, toTimeZone);
			
		}catch(Exception e) {
			logger.error("Exception occured at method convertTimeZone and exception is ->", e);
		}
		
		return new Date(date.getTime()
				+ (toTimeZoneOffset - fromTimeZoneOffset));
	}

	/**
	 * Calculates the offset of the <code>timeZone</code> from UTC, factoring in
	 * any additional offset due to the time zone being in daylight savings time
	 * as of the given <code>date</code>.
	 * 
	 * @param date
	 * @param timeZone
	 * @return
	 */
	private static long getTimeZoneUTCAndDSTOffset(Date date, TimeZone timeZone) {
		long timeZoneDSTOffset = 0;
		if (timeZone.inDaylightTime(date)) {
			timeZoneDSTOffset = timeZone.getDSTSavings();
		}

		return timeZone.getRawOffset() + timeZoneDSTOffset;
	}

	// Load foreman name
	public String getForemanName(int userId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		String foremanName = "No Foreman";
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "select usrName from ff_user_management where usrRole=? and usrId=?";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setString(1, "Foreman");
				prepStmnt.setInt(2, userId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {

					foremanName = getString(resInfo, "usrName", "");

				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return foremanName;
	}

	// Load client name
	public String getClientName(int clientId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		String foremanName = "No Foreman";
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "select clientName from ff_client where  clientId=?";
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, clientId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {

					foremanName = getString(resInfo, "clientName", "");

				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return foremanName;
	}

	public String getNoImagePath() {
		FacesContext ctx = FacesContext.getCurrentInstance();
		String path = "";
		if (ctx == null) {
			path = MessageLoader.getInstance().getMessageStatement(
					"MSG_EMAIL_URL_LINK");
		} else {
			path = ctx.getExternalContext().getRealPath(File.separator);
			if (path.charAt(path.length() - 1) != File.separatorChar) {
				path += File.separator;
			}
		}
		String logopath1 = path + "resources" + File.separator + "assets"
				+ File.separator + "img" + File.separator + "no-image-icon.png";
		return logopath1;
	}

	public InputStream getNoImageStream() {
		InputStream is = null;
		try {
			File file = new File(getNoImagePath());
			is = new FileInputStream(file);
		} catch (Exception ex) {
			is = null;
			logger.error("Error n getting no image stream");
		}

		return is;
	}

	public InputStream getNoUImageThumbnilStream() {

		InputStream is = null;
		BufferedImage buffOrigImage = null;
		try {
			buffOrigImage = ImageIO.read(new File(getNoImagePath()));
			BufferedImage thumbnailImage = CommonDAO
					.generateThumbnailImageByBufferedImage(buffOrigImage);
			org.apache.commons.io.output.ByteArrayOutputStream os = new org.apache.commons.io.output.ByteArrayOutputStream();
			//MVIR Starts
			//ImageIO.write(thumbnailImage, "png", os);
			thumbnailImage = getFormattedImage(thumbnailImage);
			ImageIO.write(thumbnailImage, "jpg", os);
			//MVIR Ends
			is = new ByteArrayInputStream(os.toByteArray());
		} catch (IOException e) {
			is = null;
			logger.error("Error in reading image", e);
		}
		return is;
	}

	public static String getTwoDigitYearFromDate(String strDate) {
		SimpleDateFormat fmt = new SimpleDateFormat(CommonDAO.dateTimeFormat);
		SimpleDateFormat yrfmt = new SimpleDateFormat(CommonDAO.yearFormat);
		Date currDate = new Date();
		try {
			currDate = fmt.parse(strDate);
		} catch (Exception e) {
			logger.error("Ëxception in getTwoDigitYearFromDate : ", e);
		}
		return yrfmt.format(currDate);
	}

	public static boolean isFileSizeGreaterThanLimit(double fileSizeinBytes,
			double fileLimit) {
		boolean isFileSizeGreater = false;
		if (fileSizeinBytes > 0) {
			double inKB = fileSizeinBytes / 1024;
			double fileSizeinMB = inKB / 1024;
			if (fileSizeinMB > fileLimit) {
				isFileSizeGreater = true;
			}
		}
		return isFileSizeGreater;
	}

	public static synchronized String getDateForTimeZoneAsString(Date dbDate,
			String timezoneId, String format) {
		DateTimeFormatter fmt = DateTimeFormat.forPattern(format);
		DateTime origDate = new DateTime(dbDate);
		DateTime dtTz = origDate.withZone(DateTimeZone.forID(timezoneId));
		String dateForTz = dtTz.toString(fmt);
		return dateForTz;
	}

	public static String getDateforTimeZoneAsDate(String date, String timeZone,
			String format) {

		Date dateConverted = null;
		DateFormat dateFormatter = new SimpleDateFormat(format);
		try {
			dateConverted = dateFormatter.parse(date);
		} catch (Exception ex) {
			logger.error("Error in parsing the date from ", ex);
			dateConverted = new Date();
		}
		dateConverted = convertTimeZone(dateConverted, timeZone,
				UserUtil.getUserTimezone(true));

		return dateFormatter.format(dateConverted);
	}

	public static Long getDateforTimeAsLong(String date, String format) {

		Long dateinLong = null;

		DateFormat formatter = new SimpleDateFormat(format);
		Date dateTime = null;
		try {
			dateTime = formatter.parse(date);
		} catch (Exception ex) {
			logger.error(
					"Error in parsing date from string to covnert to long ", ex);
		}

		if (dateTime != null) {
			dateinLong = dateTime.getTime();
		}

		return dateinLong;
	}

	/**
	 * Creating Client Work order Request Number
	 * 
	 * @param companyId
	 */
	public synchronized String getWORequestNumber(Integer companyId, String year) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		String Reqnum = "";
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		int count = 0;

		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = DBQueryLoader.getInstance()
						.getQueryStatement(
								"SELECT_COUNT_CLIENT_WORKORDER_BY_COMPANYID");
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				prepStmnt.setInt(2, companyId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next())
					count = resInfo.getInt("Total");
				count = count + 1;
				NumberFormat formatter = new DecimalFormat("#0000");
				Reqnum = WO + year + formatter.format(count);
			} catch (SQLException scon) {
				logger.error(SQLException + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error(exception + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}

		} while (retry);
		return Reqnum;
	}

	public static synchronized String getDateAsStringfromResultSet(
			ResultSet resInfo, String columnName, String format) {
		String formattedDate = "";
		Date date = null;
		try {
			date = resInfo.getTimestamp(columnName);
			SimpleDateFormat dateFormat = new SimpleDateFormat(format);
			formattedDate = dateFormat.format(date);
		} catch (Exception ex) {
			logger.error("Error in getting date from column: " + columnName, ex);
		}
		return formattedDate;

	}

	public static Date getDateFromString(String date_str, String format) {

		Date date = null;
		if (date_str.equals("0")) {
			return null;
		}
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		try {
			date = formatter.parse(date_str);
		} catch (Exception ex) {
			logger.error("Error in getting date from string", ex);
		}
		return date;
	}

	// load proposal foreman name
	public List<SelectItem> loadForemanListforAssigedWOinCompany(
			Integer companyId, Integer propertyId) {
		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		int i = 0;
		List<SelectItem> foremanList = new ArrayList<SelectItem>();
		
		//v1.6 Starts
		boolean isNeedToBeFiltered = false;
		LoginBean loginBean = null;
		//v1.6 Ends
		
		do {
			retry = false;
			try {
				
				con = db2Connector.getConnection(true);
				StringBuilder selectQryBuilder = new StringBuilder(
						"SELECT DISTINCT(wolForemanAssignedId),usrId,usrName,wolRowStatus,wohRowStatus FROM ff_workorder_line "
								+ "JOIN ff_workorder_header ON wolWohId=wohId "
								+ "JOIN ff_user_management ON wolForemanAssignedId=usrId AND usrRowStatus IS NULL  "
								+ "WHERE wohRowStatus IS NULL AND  wolRowStatus IS NULL AND wohComId=?");

				if (propertyId > 0) {
					selectQryBuilder
							.append(" AND wolForemanAssignedId IN (SELECT cfUsrId FROM ff_client_foreman_management WHERE cfCmId=?) ");
				}
				//v1.6 Starts
				else {
					if(this.beanObject instanceof WorkorderInvoiceReportBean) {
						WorkorderInvoiceReportBean workorderInvoiceReportBean = (WorkorderInvoiceReportBean) beanObject;
						loginBean = workorderInvoiceReportBean.getLoginBean();
					}
					
					ForemanUtil foremanUtil = new ForemanUtil();
					isNeedToBeFiltered = foremanUtil.getForemanFilterByAssgdProperties(loginBean.getUserId(),
							loginBean.getCacheUserRole(), moduleName);
					
					if(isNeedToBeFiltered) {
						selectQryBuilder.append("  AND wolForemanAssignedId IN (SELECT cfUsrId FROM ff_client_foreman_management WHERE cfCmId in "
								+ "( SELECT cfCmId FROM ff_client_foreman_management WHERE cfUsrId = ?)) ");
					}
				}
				//v1.6 Ends

				String selectStatement = selectQryBuilder.toString();
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				
				if (propertyId > 0) {
					prepStmnt.setInt(2, propertyId);
				}
				
				//v1.6 Starts
				if(isNeedToBeFiltered) {
					prepStmnt.setInt(2, loginBean.getUserId());
				}
				//v1.6 Ends
				
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					foremanList.add(new SelectItem(
							"" + resInfo.getInt("usrId"), getString(resInfo,
									"usrName", "")));
					i++;
				}
				if (i == 0) {
					foremanList.add(new SelectItem(0, "No Foreman"));
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return foremanList;
	}

	public static String getImageListAsString(JSONArray jsonArray) {

		StringBuilder multipleImageString = new StringBuilder("");

		for (int index = 0; index < jsonArray.length(); index++) {
			try {
				multipleImageString.append(jsonArray.getString(index));
				multipleImageString
						.append(CommonVariables.MULTIPLE_IMAGE_SPLITTER_TAG);
			} catch (JSONException ex) {
				logger.error("Error in extracting image from JSON Array ", ex);
			} catch (Exception ex) {
				logger.error("Error in extracting image from JSON Array ", ex);
			}
		}

		return multipleImageString.toString();
	}
	//v1.16 Starts
	public static String getAWSImageListAsString(JSONArray jsonArray, String type) {
		//logger.info("getAWSImageListAsString: "+jsonArray.toString());
		StringBuilder multipleImageString = new StringBuilder("");
		for (int index = 0; index < jsonArray.length(); index++) {
			try {
				JSONObject explrImageObject = jsonArray.getJSONObject(index);
				//logger.info("getAWSImageListAsString OBJECT multipleImageString: "+explrImageObject.toString());				
				if(explrImageObject.optString("imageAWS").length() > 0 ) {
					multipleImageString.append(explrImageObject.optString("imageAWS"));
					multipleImageString.append(",");
					
					final String module = type;
					final String awsImageURL = explrImageObject.optString("imageAWS").replace(URL_TO_HIDE, "");
					
					if (!StringUtils.isEmpty(explrImageObject.optString("imageAWS"))) {
						
						/**
						 * This piece of code is used to find whether the uploaded image is
						 * exist or not in S3 bucket. If not exception will be added in logging
						 * So pager duty alert will be triggered
						 */
						
						executorService.execute(new Runnable() {
							public void run() {
								boolean isAwsImageExist = S3Util.doesObjectExist(awsImageURL, MOBILE_AWS_BUKCET);
								if (!isAwsImageExist) {
									logger.error("AWSImageMissingException - Image is missing for following key ->"
											+ awsImageURL + " and the module is ->" + module);
								} else {
									logger.error("Image is exist ->" + awsImageURL);
								}
							}
						});

					}
				}else {
					multipleImageString.append("");
					multipleImageString.append(",");
				}
				
				if(explrImageObject.optString("originalSizeKB").length() > 0) {
					multipleImageString.append(explrImageObject.optString("originalSizeKB"));
					multipleImageString.append(",");
				}else {
					multipleImageString.append("0");
					multipleImageString.append(",");
				}
				
				if(explrImageObject.optString("defaultCompressSizeKB").length() > 0) {
					multipleImageString.append(explrImageObject.optString("defaultCompressSizeKB"));
					multipleImageString.append(",");
				}else {
					multipleImageString.append("0");
					multipleImageString.append(",");
				}
				
				if(explrImageObject.optString("isSpectrumCompressed").length() > 0) {
					multipleImageString.append(explrImageObject.optString("isSpectrumCompressed"));
					multipleImageString.append(",");
				}else {
					multipleImageString.append("NO");
					multipleImageString.append(",");
				}
				
				if(explrImageObject.optString("compressedSizeKB").length() > 0) {
					multipleImageString.append(explrImageObject.optString("compressedSizeKB"));
					multipleImageString.append(",");
				}else {
					multipleImageString.append("0");
					multipleImageString.append(",");
				}
				
				//v1.17 Starts
				if(explrImageObject.optString("imageWidth").length() > 0) {
					multipleImageString.append(explrImageObject.optString("imageWidth"));
					multipleImageString.append(",");
				}else {
					multipleImageString.append("0");
					multipleImageString.append(",");
				}
				
				if(explrImageObject.optString("imageHeight").length() > 0) {
					multipleImageString.append(explrImageObject.optString("imageHeight"));
					multipleImageString.append(",");
				}else {
					multipleImageString.append("0");
					multipleImageString.append(",");
				}
				
				if(explrImageObject.optString("specturmCompressDuration").length() > 0) {
					multipleImageString.append(explrImageObject.optString("specturmCompressDuration"));
					multipleImageString.append(",");
				}else {
					multipleImageString.append("0");
					multipleImageString.append(",");
				}
				
				if(type.equalsIgnoreCase("ASSET")) {
					if(explrImageObject.optString("multipleImageLineId").length() > 0) {
						multipleImageString.append(explrImageObject.optString("multipleImageLineId"));
						multipleImageString.append(",");
					}else {
						multipleImageString.append("0");
						multipleImageString.append(",");
					}
				}
				
				if(explrImageObject.optString("uploadAWSDuration").length() > 0) {
					multipleImageString.append(explrImageObject.optString("uploadAWSDuration"));
					multipleImageString.append(CommonVariables.MULTIPLE_IMAGE_SPLITTER_TAG);
				}else {
					multipleImageString.append("0");
					multipleImageString.append(CommonVariables.MULTIPLE_IMAGE_SPLITTER_TAG);
				}
				
				//v1.17 Ends
			} catch (JSONException ex) {
				logger.error("Error in getAWSImageListAsString image from JSON Array ", ex);
			} catch (Exception ex) {
				logger.error("Error in getAWSImageListAsString image from JSON Array ", ex);
			}
		}
		//logger.info("multipleImageString: "+multipleImageString.toString());
		return multipleImageString.toString();
	}
	//v1.16 Ends
	

	public Integer getCompanyIdByForemanId(Integer foremanId) {

		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		int i = 0;
		Integer companyId = 0;
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "SELECT usrComId as companyId FROM ff_user_management WHERE usrId=? "
						//v1.0 Starts
						//+ " AND usrRole='Foreman' ";
						//v1.8 Starts
						//+ " AND (usrRole='Foreman' or usrRole='Super Admin')";
						+ " AND (usrRole='Foreman' or usrRole='Super Admin' or usrRole='Admin')";
						//v1.8 Ends
						//v1.0 Ends
				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, foremanId);
				resInfo = prepStmnt.executeQuery();
				if (resInfo != null && resInfo.next()) {
					companyId = getInt(resInfo, "companyId", 0);
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);

		return companyId;
	}

	public static Integer getDemoCompanyId() {
		Integer demoCompanyId = 0;
		try {
			demoCompanyId = Integer.valueOf(MessageLoader.getInstance()
					.getMessageStatement("MSG_DEMO_COMPANY_ID"));
		} catch (Exception ex) {
			logger.error("Exception in getting demoCompanyId ", ex);
			demoCompanyId = 0;
		}
		return demoCompanyId;
	}

	public static String maskPhoneNumber(String phoneNumber) {

		String maskedPhoneNumber = "";

		try {
			maskedPhoneNumber += "(" + phoneNumber.substring(0, 3) + ") "
					+ phoneNumber.substring(3, 6) + "-"
					+ phoneNumber.substring(6);

		} catch (Exception ex) {

			logger.error("Error in masking phone number ", ex);
			return phoneNumber;

		}

		return maskedPhoneNumber;

	}

	public static int daysBetween(Calendar day1, Calendar day2) {
		Calendar dayOne = (Calendar) day1.clone(), dayTwo = (Calendar) day2
				.clone();

		if (dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR)) {
			return Math.abs(dayOne.get(Calendar.DAY_OF_YEAR)
					- dayTwo.get(Calendar.DAY_OF_YEAR));
		} else {
			if (dayTwo.get(Calendar.YEAR) > dayOne.get(Calendar.YEAR)) {
				// swap them
				Calendar temp = dayOne;
				dayOne = dayTwo;
				dayTwo = temp;
			}
			int extraDays = 0;

			int dayOneOriginalYearDays = dayOne.get(Calendar.DAY_OF_YEAR);

			while (dayOne.get(Calendar.YEAR) > dayTwo.get(Calendar.YEAR)) {
				dayOne.add(Calendar.YEAR, -1);
				// getActualMaximum() important for leap years
				extraDays += dayOne.getActualMaximum(Calendar.DAY_OF_YEAR);
			}

			return extraDays - dayTwo.get(Calendar.DAY_OF_YEAR)
					+ dayOneOriginalYearDays;
		}
	}

	public String getCompanyLogoS3URLbyId(Integer companyId) {

		String companyS3URLLogo = "";

		try {
			String companyLogoString = getCompanyLogoById(companyId);

			if (companyLogoString != null && companyLogoString.length() > 3) {

				companyS3URLLogo = S3_BUCKET_HOME + COMPANY_S3_URL + "/"
						+ companyId + ".png";
			}

		} catch (Exception ex) {
			logger.error("Error in getting company S3 URL ", ex);
		}

		return companyS3URLLogo;
	}

	public String getCappsureMailLogoURL() {
		String cappsureLogoURL = "";

		try {

			cappsureLogoURL = MessageLoader.getInstance().getMessageStatement(
					"MSG_S3_BUCKET_CAPPSURE_LOGO_EMAIL_POWERED_BY");

		} catch (Exception ex) {
			logger.error("Error in getting cappure logo URL");
		}

		return cappsureLogoURL;
	}

	public String getNumberofDaysDiff(Date createdDate, boolean isSinceToday) {

		SimpleDateFormat dateFormat = new SimpleDateFormat(dateTimeFormat);

		Integer totalTrailDays = new Integer(MessageLoader.getInstance()
				.getMessageStatement("MSG_FREE_TRIAL_TOTAL_DAYS"));

		String createdDateFormat = getUserDateFormat(createdDate);

		String todayDateFormat = getUserDateFormat(new Date());

		Calendar day1 = Calendar.getInstance();
		Calendar day2 = Calendar.getInstance();

		try {
			day1.setTime(dateFormat.parse(todayDateFormat));
			day2.setTime(dateFormat.parse(createdDateFormat));
		} catch (Exception ex) {
			logger.error("Exception in parsing date ", ex);
		}

		if (isSinceToday) {
			return "" + (CommonDAO.daysBetween(day1, day2));
		}

		return "" + (totalTrailDays - CommonDAO.daysBetween(day1, day2));
	}

	public String getUserDateFormat(Date date) {
		String dateFormat = "";
		if (date != null) {
			try {
				dateFormat = CommonDAO.getDateForTimeZoneAsString(date,
						UserUtil.getUserTimezone(false));
			} catch (Exception e) {
				SimpleDateFormat fmt = new SimpleDateFormat(
						CommonDAO.dateTimeFormat);
				dateFormat = fmt.format(date);
			}
		}

		return dateFormat;
	}

	public static boolean isWalkMeCompany(Integer companyId) {
		Integer walkMeCompanyId = -1;
		boolean isWalkMeCompany = false;
		try {
			walkMeCompanyId = new Integer(MessageLoader.getInstance()
					.getMessageStatement("MSG_WALK_ME_COM_ID"));

			if (walkMeCompanyId.equals(companyId)) {
				return true;
			}
		} catch (Exception ex) {
			logger.error("Error in getting walk me companyId ");
		}

		return isWalkMeCompany;
	}

	public static Date getWalkMeCompanyCraetedTime() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				dateTimeFormatSQL);
		Date date = null;
		try {
			String walkMeCreatedDate = MessageLoader.getInstance()
					.getMessageStatement("MSG_WALK_ME_CREATED_DATE");
			date = simpleDateFormat.parse(walkMeCreatedDate);
		} catch (Exception ex) {
			logger.error("Error in getting Walk ME created date");
		}

		return date;
	}

	public static String removeHypen(String foremanMobileNumber) {

		if (foremanMobileNumber == null
				|| foremanMobileNumber.equals(CommonVariables.EMPTY_STRING)) {
			return foremanMobileNumber;
		}

		try {
			foremanMobileNumber = foremanMobileNumber.replaceAll("-", "");
		} catch (Exception ex) {
			logger.error("Exception in removing hypen from mobile number ", ex);
		}

		return foremanMobileNumber;
	}
	
	//v1.1 Starts
	
	/**
	 * This method used to prevent
	 * clumsy image compression of PNG
	 * When png image compressed to JPG
	 * Image will look bad.
	 * Hence this approach is implemented 
	 * @param bufferedImage
	 * @return BufferedImage
	 */
	public static BufferedImage getFormattedImage(BufferedImage bufferedImage){
		BufferedImage newBufferedImage =null;
		try{
			if(bufferedImage!=null){
				newBufferedImage = new BufferedImage(bufferedImage.getWidth(),
						bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
				 newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.WHITE, null);
			}else{
				return bufferedImage;
			}
			 
		}catch(Exception e){
			logger.error("Exception in get formatted image ", e);
		}
		return newBufferedImage;
	}
	//v1.1 Ends
	
	//v1.4 Starts
	/*public HashMap<Integer, String> getListOfCoordinates(int foremanId){
		
		Integer propertyId = 0;
		String coordinates = null;
		String polygonCoordinates = null;

		HashMap<Integer, String> coordinatesMap = null;
		
		DBConnector db2Connector = DBConnector.getInstance();
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
			try {
				conn = db2Connector.getConnection(true);
				if(foremanId>0 && conn!=null && !conn.isClosed()) { 
					
					logger.info("Fetching top 20 properties process is started for following Foreman ->"+foremanId);
					
					coordinatesMap = new HashMap<Integer, String> ();
					String statement = " SELECT cmId,polygonCoordinates, cmMapNorthEast, cmMapSouthWest FROM "
							+ " ff_client_foreman_management		"
							+ " left join ff_client_management AS prop on prop.cmId=cfCmId "
							+ "	left join ff_company_management on prop.cmComId=comId"
							+ " left join ff_asset_management_header on prop.cmId=assetPropertyId "
							+ " where cfUsrId= ? and cmRowStatus is null  "
							+ "	and (cmVendorStatus='' or cmVendorStatus is null) "
							+ " and comRowStatus is null and comActiveStatus=1 group by cmId ";
					preparedStatement = conn.prepareStatement(statement);
					preparedStatement.setInt(1, foremanId);
					resultSet = preparedStatement.executeQuery();
					while(resultSet.next()) {
						propertyId = resultSet.getInt("cmId");
						
						polygonCoordinates = resultSet.getString("polygonCoordinates");
						coordinates = resultSet.getString("cmMapNorthEast")+"__"+resultSet.getString("cmMapSouthWest");
						if(polygonCoordinates !=null && !polygonCoordinates.trim().isEmpty()) {
							coordinatesMap.put(propertyId, polygonCoordinates);
						}else {
							coordinatesMap.put(propertyId, coordinates);
						}
						
					}
					
					logger.info("Fetching top 20 properties process is completed and the number of properties is ->"+coordinatesMap.size());
				}
				
			} catch (Exception e) {
				logger.error("Exception occured in getListOfCoordinates() "); 
			} finally {
				close(resultSet);
				close(preparedStatement);
				close(conn);
			}

		return coordinatesMap;
	
	}*/
	
	/**
	 * This method is used to maintain
	 * the foreman activities in properties
	 * Which is used to track the what are 
	 * all the properties he is working
	 * If the user record is exist
	 * then the updating will be done
	 * Else new record will be insert into table
	 * 
	 * @param activityDTO
	 * @param tableName
	 */
	public List<Integer> maintainForemanActivityLog(ForemanMaintainActivityDTO activityDTO, String tableName){
		
		logger.info("Maintaining foreman activity into database method invoked");
		
		int count = 0;
		int insertCount = 0;
		int updateCount = 0;

		DBConnector db2Connector = DBConnector.getInstance();
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		
		PreparedStatement prepStmtToUpdate = null;
		
		ResultSet resultSet = null;
		
		List<Integer> activityIds = null;
		
			try {
				conn = db2Connector.getConnection(true);
				
				if(activityDTO!=null && conn!=null && !conn.isClosed()) {
					
					String insertQuery = " insert into "+tableName+" (foremanId, deviceId, deviceType, propertyId,"
										+ " locationLat, locationLng, accuracy, batteryLevel, locationFetchTime, inTime, "
										+ " lastUpdatedTime, createdBy, createdTime, backgroundPings, foregroundPings, appVersion, uploadSpeed, downloadSpeed) values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), ?, now(), ?, ?, ?, ?, ?) ";
					
					String updateQuery = "update "+tableName+" set locationLat = ?, locationLng = ?, lastUpdatedTime = now(), batteryLevel = ?, backgroundPings = (ifnull(backgroundPings,0)+?),"
										+ " foregroundPings = (ifnull(foregroundPings,0)+?), appVersion = ?,  uploadSpeed =?, downloadSpeed =? where foremanId = ? and propertyId = ? " 
										+ " and deviceId= ? and deviceType = ? and outTime is null";
					
					preparedStatement = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
					
					prepStmtToUpdate = conn.prepareStatement(updateQuery);
					
					logger.info("Insert and updating the Foreman properties details process is started");
					
					/**
					 * This looping process is used to add the set of query
					 * with update and insert the foreman details
					 * with matched properties.
					 */
					
					for (CompositeKey key:activityDTO.getListOfMatchedProperties()) {
						Integer propertyId = Integer.valueOf(key.getKey1());
						activityDTO.setPropertyId((propertyId));
						Integer foremanId = Integer.valueOf(key.getKey2());
						activityDTO.setUserId(foremanId);
						
						logger.info("The property Id to be inserted/updated is :" + propertyId);
						logger.info("The foreman Id to be inserted is :" + foremanId);
						
						/**
						 * This condition is used to find and update
						 * whether Foreman details are exist with
						 * specific property in table
						 */
						
						if(selectForemanisActiveOrNot(activityDTO, tableName)) {
							
							prepStmtToUpdate.setDouble(++count, activityDTO.getLocationLat());
							prepStmtToUpdate.setDouble(++count, activityDTO.getLocationLng());
							
							prepStmtToUpdate.setInt(++count, activityDTO.getBatteryLevel());
							prepStmtToUpdate.setInt(++count, activityDTO.getApplicationStatus().
									trim().equalsIgnoreCase(CommonVariables.APPLICATION_STATUS_BACKGROUND)?1:0);
							prepStmtToUpdate.setInt(++count, activityDTO.getApplicationStatus().
									trim().equalsIgnoreCase(CommonVariables.APPLICATION_STATUS_FOREGROUND)?1:0);
							prepStmtToUpdate.setDouble(++count, activityDTO.getAppVersion());
							prepStmtToUpdate.setString(++count, activityDTO.getUploadSpeed());	//v1.15
							prepStmtToUpdate.setString(++count, activityDTO.getDownloadSpeed());	//v1.15
							prepStmtToUpdate.setInt(++count, activityDTO.getUserId());
							prepStmtToUpdate.setInt(++count, activityDTO.getPropertyId());
							prepStmtToUpdate.setString(++count, activityDTO.getDeviceId());
							prepStmtToUpdate.setString(++count, activityDTO.getDeviceType());

							
							prepStmtToUpdate.addBatch();
						}else {
							preparedStatement.setInt(++count, foremanId);
							preparedStatement.setString(++count, activityDTO.getDeviceId());
							preparedStatement.setString(++count, activityDTO.getDeviceType());
							preparedStatement.setInt(++count, activityDTO.getPropertyId());
							preparedStatement.setDouble(++count, activityDTO.getLocationLat());
							preparedStatement.setDouble(++count, activityDTO.getLocationLng());
							preparedStatement.setInt(++count, activityDTO.getAccuracy());
							preparedStatement.setInt(++count, activityDTO.getBatteryLevel());
							preparedStatement.setTimestamp(++count, new java.sql.Timestamp(activityDTO.getLocationFetchTime()));
							preparedStatement.setInt(++count, activityDTO.getUserId());
							preparedStatement.setInt(++count, activityDTO.getApplicationStatus().
									trim().equalsIgnoreCase(CommonVariables.APPLICATION_STATUS_BACKGROUND)?1:0);
							preparedStatement.setInt(++count, activityDTO.getApplicationStatus().
									trim().equalsIgnoreCase(CommonVariables.APPLICATION_STATUS_FOREGROUND)?1:0);
							preparedStatement.setDouble(++count, activityDTO.getAppVersion());
							preparedStatement.setString(++count, activityDTO.getUploadSpeed());	//v1.15
							preparedStatement.setString(++count, activityDTO.getDownloadSpeed());	//v1.15
							preparedStatement.addBatch();
						}
						count = 0;
						
					}
					
					logger.info("Insert and updating the Foreman properties details process is completed");
					
					insertCount = preparedStatement.executeBatch().length;
					updateCount = prepStmtToUpdate.executeBatch().length;
					
					resultSet = preparedStatement.getGeneratedKeys();
					
					if(insertCount>0) {
						
						activityIds = new ArrayList<Integer>();
						
						logger.info("Inserted the number of property details ->"+insertCount);
						
						while(resultSet.next()) {
							activityIds.add(resultSet.getInt(1));
						}
					}
					
					logger.info("Processed the number of rows to insert in "+tableName+" table ->"+insertCount);
					logger.info("Processed the number of rows to update in "+tableName+" table ->"+updateCount);
					
				}
				
			} catch (Exception e) {
				logger.error("Exception occured in insertForemanActivityLog()", e); 
			} finally {
				close(resultSet);
				close(preparedStatement);
				close(prepStmtToUpdate);
				close(conn);
			}
			
			return activityIds;
	}
	
	public ForemanMaintainActivityDTO getActivityDetail(int activityId){
		
		int count = 0;
		
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		
		ForemanMaintainActivityDTO activityDTO = null;
		
		DBConnector db2Connector = DBConnector.getInstance();
			try {
				conn = db2Connector.getConnection(true);
				
				if(conn!=null && !conn.isClosed()) {
					
					
					String statement = "select recordNo, foremanId, deviceId, deviceType, propertyId, locationLat, locationLng, inTime, outTime, lastUpdatedTime, Timestampdiff(minute, inTime, outTime) as timeInPropertyInMinutes from ff_foreman_activity_log_mas where recordNo = ?";
					
					preparedStatement = conn.prepareStatement(statement);
					
					preparedStatement.setInt(++count, activityId);
					
					resultSet = preparedStatement.executeQuery();
					
					while(resultSet.next()) {
						activityDTO = new ForemanMaintainActivityDTO();
						
						activityDTO.setRecordNo(resultSet.getInt(CommonVariables.RECORD_NO));
						activityDTO.setUserId(resultSet.getInt(CommonVariables.ACTIVITY_FOREMAN_ID));
						activityDTO.setDeviceId(resultSet.getString(CommonVariables.DEVICE_ID));
						activityDTO.setDeviceType(resultSet.getString(CommonVariables.DEVICE_TYPE));
						activityDTO.setPropertyId(resultSet.getInt(CommonVariables.PROPERTY_ID));
						activityDTO.setLocationLat(resultSet.getDouble(CommonVariables.LOCATION_LAT));
						activityDTO.setLocationLng(resultSet.getDouble(CommonVariables.LOCATION_LNG));
						activityDTO.setInTime(resultSet.getTimestamp(CommonVariables.IN_TIME));
						activityDTO.setOutTime(resultSet.getTimestamp(CommonVariables.OUT_TIME));
						activityDTO.setLastUpdatedTime(resultSet.getTimestamp(CommonVariables.LAST_UPDATED_TIME));
						activityDTO.setDurationInHours(resultSet.getInt(CommonVariables.TIME_IN_PROPERTY_IN_MINUTES));
						
					}
					
				}
				
			} catch (Exception e) {
				logger.error("Exception occured in selectForemanListWhoAreNotActive() -> ", e); 
			} finally {
				close(resultSet);
				close(preparedStatement);
				close(conn);
			}
 
		return activityDTO;
	}
	
	public List<ForemanMaintainActivityDTO> getActivityDetailByCompany(int foremanId, int companyId){
		
		int count = 0;
		
		boolean isNeedToBeFiltered = false;
		
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		
		ForemanMaintainActivityDTO activityDTO = null;
		
		List<ForemanMaintainActivityDTO> activityDTOList = null;
		
		DBConnector db2Connector = DBConnector.getInstance();
			try {
				conn = db2Connector.getConnection(true);
				
				if(conn!=null && !conn.isClosed()) {
					
					activityDTOList = new ArrayList<ForemanMaintainActivityDTO>();
					
					
					String statement = "select recordNo, foremanId, deviceId, deviceType, propertyId, locationLat, locationLng, inTime, outTime, lastUpdatedTime, Timestampdiff(minute, inTime, outTime) as timeInPropertyInMinutes, usrName, cmPropertyName, cmMapSouthWest, cmMapNorthEast, polygonCoordinates "
							+ " from ff_foreman_activity_log_mas, ff_user_management, ff_client_management "
							+ " where outTime is null and foremanId = usrId and usrComId = ?"
							+ " and propertyId = cmId";
					
					
					isNeedToBeFiltered = foremanId>0?new ForemanUtil().getForemanFilterByAssgdProperties(foremanId,
							CommonVariables.FOREMAN, CommonVariables.PROPERTY_FILTER_BY_TOGGLE):isNeedToBeFiltered;
					
					if(isNeedToBeFiltered) {
						statement += " and cmId in (select cfCmId from ff_client_foreman_management where cfUsrId = ?) ";
					}
					
					preparedStatement = conn.prepareStatement(statement);
					
					preparedStatement.setInt(++count, companyId);
					
					
					if(isNeedToBeFiltered) {
						preparedStatement.setInt(++count, foremanId);
					}
					
					resultSet = preparedStatement.executeQuery();
					
					while(resultSet.next()) {
						activityDTO = new ForemanMaintainActivityDTO();
						
						activityDTO.setRecordNo(resultSet.getInt(CommonVariables.RECORD_NO));
						activityDTO.setUserId(resultSet.getInt(CommonVariables.ACTIVITY_FOREMAN_ID));
						activityDTO.setDeviceId(resultSet.getString(CommonVariables.DEVICE_ID));
						activityDTO.setDeviceType(resultSet.getString(CommonVariables.DEVICE_TYPE));
						activityDTO.setPropertyId(resultSet.getInt(CommonVariables.PROPERTY_ID));
						activityDTO.setLocationLat(resultSet.getDouble(CommonVariables.LOCATION_LAT));
						activityDTO.setLocationLng(resultSet.getDouble(CommonVariables.LOCATION_LNG));
						activityDTO.setInTime(resultSet.getTimestamp(CommonVariables.IN_TIME));
						activityDTO.setOutTime(resultSet.getTimestamp(CommonVariables.OUT_TIME));
						activityDTO.setLastUpdatedTime(resultSet.getTimestamp(CommonVariables.LAST_UPDATED_TIME));
						activityDTO.setDurationInHours(resultSet.getInt(CommonVariables.TIME_IN_PROPERTY_IN_MINUTES));
						
						activityDTO.setUserName(resultSet.getString(CommonVariables.DB_USER_NAME));
						activityDTO.setPropertyName(resultSet.getString(CommonVariables.DB_PROPERTY_NAME));
						activityDTO.setNorthEastCoordinates(resultSet.getString(CommonVariables.CM_MAP_NORTH_EAST));
						activityDTO.setSouthWestCoordinates(resultSet.getString(CommonVariables.CM_MAP_SOUTH_WEST));
						activityDTO.setPolygonCoordinates(resultSet.getString(CommonVariables.POLYGON_COORDINATES));
						
						activityDTOList.add(activityDTO);
					}
					
				}
				
			} catch (Exception e) {
				logger.error("Exception occured in selectForemanListWhoAreNotActive() -> ", e); 
			} finally {
				close(resultSet);
				close(preparedStatement);
				close(conn);
			}
 
		return activityDTOList;
	}
	
	/**
	 * 
	 * This method is used to 
	 * get the all foreman details
	 * who are all not active more than
	 * or equal to 3 minutes
	 * 
	 * Note: This method is used for background thread service
	 * Hence fetched all the inactive foreman for performance
	 * 
	 * @return List<ForemanMaintainActivityDTO>
	 */
	
	public List<ForemanMaintainActivityDTO> selectForemanListWhoAreNotActive(){
		
		logger.info("Fetching list of foreman who are not active in properties process is started.");
		
		ForemanMaintainActivityDTO activityDTO = null;
		
		List<ForemanMaintainActivityDTO> inactiveForemanList = null;
		
		DBConnector db2Connector = DBConnector.getInstance();
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
			try {
				conn = db2Connector.getConnection(true);
				if(conn!=null && !conn.isClosed()) {
					
					inactiveForemanList = new ArrayList<ForemanMaintainActivityDTO>();
					
					String statement = "select recordNo, foremanId, deviceId, deviceType, propertyId from ff_foreman_activity_log_mas where timestampdiff(MINUTE, lastUpdatedTime, now())>=10 and outTime is null";
					
					preparedStatement = conn.prepareStatement(statement);
					resultSet = preparedStatement.executeQuery();
					
					while(resultSet.next()) {
						activityDTO = new ForemanMaintainActivityDTO();
						
						activityDTO.setRecordNo(resultSet.getInt("recordNo"));
						activityDTO.setUserId(resultSet.getInt("foremanId"));
						activityDTO.setDeviceId(resultSet.getString("deviceId"));
						activityDTO.setDeviceType(resultSet.getString("deviceType"));
						activityDTO.setPropertyId(resultSet.getInt("propertyId"));
						
						inactiveForemanList.add(activityDTO);
					}
					
					logger.info("Fetching list of foreman who are not active in properties process is completed and inactive foremans size is:"+inactiveForemanList.size());
				}else {
					logger.error("Connection is null or closed in selectForemanListWhoAreNotActive()");
				}
				
			} catch (Exception e) {
				logger.error("Exception occured in selectForemanListWhoAreNotActive() -> "+e.getMessage()); 
				e.printStackTrace();
			} finally {
				close(resultSet);
				close(preparedStatement);
				close(conn);
			}
 
		return inactiveForemanList;
	}
	
	/**
	 * This method is core of maintain
	 * Foreman activity
	 * When the foreman is inactive by 
	 * 3 minutes or more this method will
	 * update the out time as current
	 * server time. Java batch concept is used
	 * here to reduce the burden of process
	 * Following fields are used to match the conditions.
	 * 
	 * Filters:
	 * 1. User Id
	 * 2. Property Id
	 * 3. Device Id
	 * 4. Device Type
	 * 5. Record no
	 * 
	 * Note: Record no is clustered index.
	 * Because it is primary key.
	 * For better performancce this 
	 * filter has been added.
	 * 
	 * @param activityDTOList
	 * @return
	 */
	
	public int[] updateOutimeForInactiveForeman(List<ForemanMaintainActivityDTO> activityDTOList){
		
		logger.info("Updating out time for inactive foreman process is started");
		
		int count = 0;
		int[] rowsCount = null;

		DBConnector db2Connector = DBConnector.getInstance();
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
			try {
				conn = db2Connector.getConnection(true);
				if(activityDTOList!=null && activityDTOList.size()>0 && conn!=null && !conn.isClosed()) {
					String statement = " update ff_foreman_activity_log_mas set outTime = now()-interval 10 minute, updatedTime = now() "
									   + " where foremanId = ? and propertyId = ? "
									   + " and deviceId= ? and deviceType = ? and recordNo = ? and outTime is null";
										
					preparedStatement = conn.prepareStatement(statement);
					
					for (ForemanMaintainActivityDTO activityDTO:activityDTOList) {
						preparedStatement.setInt(++count, activityDTO.getUserId());
						preparedStatement.setInt(++count, activityDTO.getPropertyId());
						preparedStatement.setString(++count, activityDTO.getDeviceId());
						preparedStatement.setString(++count, activityDTO.getDeviceType());
						preparedStatement.setInt(++count, activityDTO.getRecordNo());
						
						preparedStatement.addBatch();
						count = 0;
					}
					
					rowsCount = preparedStatement.executeBatch();
					
					logger.info("Updating out time for inactive foreman process is completed and no of processed foreman is ->"+activityDTOList.size());
				}
				
			} catch (Exception e) {
				logger.error("Exception occured in updateOutimeForInactiveForeman() -> "+e.getMessage()); 
			} finally {
				close(resultSet);
				close(preparedStatement);
				close(conn);
			}

		return rowsCount;
	}
	
	//v1.5 Starts
	public int updateExpectedPings(){
		
		logger.info("Updating out time for all foreman process is started");
		
		int rowsCount = 0;

		DBConnector db2Connector = DBConnector.getInstance();
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
			try {
				conn = db2Connector.getConnection(true);
				if(conn!=null && !conn.isClosed()) {
					String statement = " update ff_foreman_activity_log_mas set expectedPings = TIMESTAMPDIFF(MINUTE, inTime, now()) where outTime is null";
										
					preparedStatement = conn.prepareStatement(statement);
					
					rowsCount = preparedStatement.executeUpdate();
					
					logger.info("Updating expected pings process is completed and no of processed foreman is ->"+rowsCount);
				}
			} catch (Exception e) {
				logger.error("Exception occured in updateExpectedPings() -> "+e.getMessage()); 
			} finally {
				close(resultSet);
				close(preparedStatement);
				close(conn);
			}

		return rowsCount;
	}
	/**
	 * This method is used to
	 * update the procurement price using quantity
	 * @param invId
	 * @param lineId
	 * @param qty
	 * @param module
	 * @return
	 */
	public int updateProcurementPrice(int invId, int lineId, BigDecimal qty, String module){
		
		logger.info("Update procurement price process is started");
		
		int count = 0;
		
		int rowsCount = 0;
		
		String query = null;

		//DBConnector db2Connector = DBConnector.getInstance();
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
			try {
				conn = ConnectionHelper.getConnection();
				if(conn!=null && !conn.isClosed()) {
					
					if(StringUtils.isNotEmpty(module)) {
						if(module.contentEquals(CommonVariables.MODULE_ASSET)) {
							query = "update ff_asset_management_line set invProcurementPrice = (select (iipPrice * ?) from ff_inventory_item where iiId =  ?) where alId = ?";
						}
						
						if(module.contentEquals(CommonVariables.MODULE_PROPOSAL)) {
							query = "update ff_proposal_line set invProcurementPrice = (select (iipPrice * ?) from ff_inventory_item where iiId =  ?) where plId = ?";
						}
						
						if(module.contentEquals(CommonVariables.MODULE_IRRIGATION)) {
							query = "update ff_green_sheet_line set invProcurementPrice = (select (iipPrice * ?) from ff_inventory_item where iiId =  ?) where gslId = ?";
						}
						
						if(module.contentEquals(CommonVariables.MODULE_WORK_ORDER)) {
							query = "update ff_workorder_line set invProcurementPrice = (select (iipPrice * ?) from ff_inventory_item where iiId =  ?) where wolId = ?";
						}						
					}
					
					//System.out.println("query for line is ->"+query);
										
					preparedStatement = conn.prepareStatement(query);
					
					preparedStatement.setBigDecimal(++count, qty);
					preparedStatement.setInt(++count, invId);
					preparedStatement.setInt(++count, lineId);
					
					rowsCount = preparedStatement.executeUpdate();
					
					logger.info("Process completed for procurement price and the result is ->"+rowsCount);
				}
			} catch (Exception e) {
				logger.error("Exception occured in updateProcurementPrice() -> "+e.getMessage()); 
			} finally {
				close(resultSet);
				close(preparedStatement);
				//close(conn);
			}

		return rowsCount;
	}
	
	/**
	 * This method is used to
	 * update the procurement price
	 * Here overload the method because need to check the connection
	 * @param assetId
	 * @param module
	 * @return
	 */
	
	public int updateProcurementPrice(int assetId, String module){
		//Here Parameter null is passing database connection
		return  updateProcurementPrice( assetId,  module, null);
	}
	
	/**
	 * This method is used to
	 * update the procurement price
	 * @param assetId
	 * @param module
	 * @param conn
	 * @return
	 */
	public int updateProcurementPrice(int assetId, String module, Connection conn){
		
		logger.info("Update header procurement price is started");
		
		int count = 0;
		
		int rowsCount = 0;
		
		String query = null;

		//DBConnector db2Connector = DBConnector.getInstance();
		//Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
			try {
				// This block is used to get the connection from connectionhelper otherwise use param connection
				if(conn==null){
					conn = ConnectionHelper.getConnection();
				}
				//conn = ConnectionHelper.getConnection();
				if(conn!=null && !conn.isClosed()) {
					
					if(StringUtils.isNotEmpty(module)) {
						if(module.contentEquals(CommonVariables.MODULE_ASSET)) {
							query = "update ff_asset_management_header set procurementPrice = (select SUM(IFNULL((select iipPrice from ff_inventory_item where iiId =  alInvId),0) * alQty) from ff_asset_management_line where alAssetId =  ? group by alAssetId) where assetId = ?";
						}
						
						if(module.contentEquals(CommonVariables.MODULE_PROPOSAL)) {
							query = "update ff_proposal_header set procurementPrice = (select SUM(IFNULL((select iipPrice from ff_inventory_item where iiId =  plInvId),0) * plQty) from ff_proposal_line where plProId =  ? group by plProId) where proId = ?";
						}
						
						if(module.contentEquals(CommonVariables.MODULE_IRRIGATION)) {
							query = "update ff_green_sheet_header set procurementPrice = (select SUM(IFNULL((select iipPrice from ff_inventory_item where iiId =  gslInvId),0) * gslQty) from ff_green_sheet_line where gslGsId =  ? group by gslGsId) where gsId = ?";
						}
						
						if(module.contentEquals(CommonVariables.MODULE_WORK_ORDER)) {
							query = "update ff_workorder_header set procurementPrice = (select SUM(IFNULL((select iipPrice from ff_inventory_item where iiId =  wolInvId),0) * wolQty) from ff_workorder_line where wolWohId =  ? group by wolWohId) where wohId = ?";
						}						
					}
					
					//System.out.println("query is ->"+query);
										
					preparedStatement = conn.prepareStatement(query);
					
					preparedStatement.setInt(++count, assetId);
					preparedStatement.setInt(++count, assetId);
					
					rowsCount = preparedStatement.executeUpdate();
					
					logger.info("Process completed for procurement price and result is ->"+rowsCount);
				}
			} catch (Exception e) {
				logger.error("Exception occured in updateProcurementPrice() -> "+e.getMessage()); 
			} finally {
				close(resultSet);
				close(preparedStatement);
				//close(conn);
			}

		return rowsCount;
	}
	//v1.5 Ends
	
	/*public int[] deleteInactiveForemanInTxnTable(List<ForemanMaintainActivityDTO> activityDTOList){
		
		int count = 0;
		int[] rowsCount = null;

		DBConnector db2Connector = DBConnector.getInstance();
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
			try {
				conn = db2Connector.getConnection(true);
				if(activityDTOList!=null && activityDTOList.size()>0 && conn!=null && !conn.isClosed()) {
					String statement = " delete from ff_foreman_activity_log_trn where foremanId = ? and propertyId = ? "
									   + " and deviceId= ? and deviceType = ? and recordNo = ? and outTime is null";
										
					preparedStatement = conn.prepareStatement(statement);
					
					for (ForemanMaintainActivityDTO activityDTO:activityDTOList) {
						preparedStatement.setInt(++count, activityDTO.getUserId());
						preparedStatement.setInt(++count, activityDTO.getPropertyId());
						preparedStatement.setString(++count, activityDTO.getDeviceId());
						preparedStatement.setString(++count, activityDTO.getDeviceType());
						preparedStatement.setInt(++count, activityDTO.getRecordNo());
						
						preparedStatement.addBatch();
						count = 0;
					}
					
					rowsCount = preparedStatement.executeBatch();
				}
				
			} catch (Exception e) {
				logger.error("Exception occured in deleteInactiveForemanInTxnTable() -> "+e.getMessage()); 
				e.printStackTrace();
			} finally {
				close(resultSet);
				close(preparedStatement);
				close(conn);
			}

		return rowsCount;
	}*/

	/**
	 * This method is used to find whether the
	 * Foreman details is exist or not for
	 * respective properties
	 * If the foreman details is exist with
	 * specific property then it will return true
	 * If not it will return as false
	 * Following fields are used to matched the
	 * conditions to find record is exist or not
	 * Filters:
	 * 
	 * 1. User Id
	 * 2. Property Id
	 * 3. Device Id
	 * 4. Device type
	 * 
	 * @param activityDTO
	 * @param tableName
	 * @return
	 */
	
	public boolean selectForemanisActiveOrNot(ForemanMaintainActivityDTO activityDTO, String tableName){
		
	//logger.info("Fetching whether the foreman is active or not method is invoked");
		
	int count = 0;
		
	boolean isForemanActiveOrNot = false;
	
	DBConnector db2Connector = DBConnector.getInstance();
	Connection conn = null;
	PreparedStatement preparedStatement = null;
	ResultSet resultSet = null;
		try {
			conn = db2Connector.getConnection(true);
			if(conn!=null && !conn.isClosed()) {
				
				//logger.info("Fetching whether the foreman is active or not method is started");
				
				String statement = " select 1 from "+tableName+" where foremanId = ? and propertyId = ? "
								  +" and deviceId= ? and deviceType = ? and outTime is null";
				
				preparedStatement = conn.prepareStatement(statement);
				preparedStatement.setInt(++count, activityDTO.getUserId());
				preparedStatement.setInt(++count, activityDTO.getPropertyId());
				preparedStatement.setString(++count, activityDTO.getDeviceId());
				preparedStatement.setString(++count, activityDTO.getDeviceType());
				
				resultSet = preparedStatement.executeQuery();
				
				if(resultSet.next()) {
					logger.info("Foreman "+activityDTO.getUserId()+" is active in following property ->"+activityDTO.getPropertyId());
					isForemanActiveOrNot = true;
				}
			}else {
				logger.error("Connection is null or closed in selectForemanListWhoAreNotActive()");
			}
			
		} catch (Exception e) {
			logger.error("Exception occured in selectForemanListWhoAreNotActive() -> "+e.getMessage()); 
		} finally {
			close(resultSet);
			close(preparedStatement);
			close(conn);
		}

	return isForemanActiveOrNot;
	}
	//v1.4 Ends
	
	//v1.6 Starts
	public ForemanTogglesMasterCtrlDTO getModuleAccessControlsFromMaster(){
		
		//logger.info("Fetching whether the foreman is active or not method is invoked");
		
		int count = 0;
		int toggleType = 0;
		String functionalKey = null;
		
		String description =null;
		
		DBConnector db2Connector = DBConnector.getInstance();
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		ResultSetMetaData metaData = null;
		
		List<String> moduleNames = null;
		
		LinkedHashMap<String, String> moduleAccessCtrlDescAndHelpText = null;
		LinkedHashMap<String, String> functionalToggleCtrlDescAndHelpText = null;
		LinkedHashMap<String, Boolean> moduleAccessCtrlMasterValues = null;
		LinkedHashMap<String, Boolean> functionalTogglesCtrlMasterValues = null;
		
		ForemanTogglesMasterCtrlDTO modAccsFuncToggsMasterCtrlDTO = null;
		
		
			try {
				conn = db2Connector.getConnection(true);
				if(conn!=null && !conn.isClosed()) {
					
					moduleNames = new ArrayList<String>();
					moduleAccessCtrlDescAndHelpText = new LinkedHashMap<String, String>();
					functionalToggleCtrlDescAndHelpText = new LinkedHashMap<String, String>();
					moduleAccessCtrlMasterValues = new LinkedHashMap<String, Boolean>();
					functionalTogglesCtrlMasterValues = new LinkedHashMap<String, Boolean>();
					
					modAccsFuncToggsMasterCtrlDTO = new ForemanTogglesMasterCtrlDTO();
					
					//logger.info("Fetching whether the foreman is active or not method is started");
					
					//v1.7 Starts
					/*String statement = " select description, workOrder, proposal, asset, irrigation, maintenance, admin, foreman, client, "
							+ " property, inventory, backOfficeReports, preferences, toggles, myAccount, toggleType, functionalKey, helpText from ff_foreman_toggles_master";*/
					
					//v1.13 Starts
					/*String statement = " select description, workOrder, proposal, asset, irrigation, maintenance, user, "
							+ " property, inventory, backOfficeReports, preferences, toggles, myAccount, workOrderRequests, toggleType, functionalKey, helpText from ff_foreman_toggles_master";*/
					
					String statement = " select description, workOrder, proposal, asset, irrigation, maintenance, user, "
							+ " property, inventory, backOfficeReports, preferences, toggles, myAccount, workOrderRequests, dashboard, invoice, paymentOption, toggleType, "
							+ " functionalKey, helpText from ff_foreman_toggles_master order by position asc";
					
					//v1.13 Ends
					//v1.7 Ends
					
					preparedStatement = conn.prepareStatement(statement);
					
					resultSet = preparedStatement.executeQuery();
					
					metaData = resultSet.getMetaData();
					
					for(int i = 1; i<=metaData.getColumnCount();i++) {
							//moduleNames.add(CommonUtil.splitCamelCase(metaData.getColumnName(i)));
						moduleNames.add(metaData.getColumnName(i));
					}
					
					
					moduleAccessCtrlDescAndHelpText.put("Module Access", MessageLoader.getInstance().getMessageStatement("MODULE_ACCESS_HELP_TEXT"));
					while(resultSet.next()) {
						
						toggleType = resultSet.getInt("toggleType");
						functionalKey = resultSet.getString("functionalKey");
						
						description = resultSet.getString("description");
						if((toggleType == 1) || (toggleType==3)) {
							moduleAccessCtrlDescAndHelpText.put(description, resultSet.getString("helpText"));
						}else {
							functionalToggleCtrlDescAndHelpText.put(description, resultSet.getString("helpText"));
						}
						
						description = WordUtils.uncapitalize(WordUtils.capitalize(description).replaceAll("\\s+",""));
						
						if((toggleType == 1) || (toggleType==3)) {
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("workOrder"));
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("proposal"));
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("asset"));
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("irrigation"));
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("maintenance"));
							//v1.7 Starts
							/*moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("admin"));
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("foreman"));
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("client"));*/
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("user"));
							//v1.7 Ends
							
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("property"));
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("inventory"));
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("backOfficeReports"));
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("preferences"));
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("toggles"));
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("myAccount"));
							//v1.7 Starts
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("workOrderRequests"));
							//v1.7 Ends
							
							//v1.14 Starts
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("dashboard"));
							//v1.14 Ends
							
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("invoice"));
							
							moduleAccessCtrlMasterValues.put(description+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("paymentOption"));
						}else if (toggleType == 2) {
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("workOrder") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("workOrder"));
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("proposal") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("proposal"));
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("asset") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("asset"));
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("irrigation") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("irrigation"));
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("maintenance") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("maintenance"));
							//v1.7 Starts
							/*functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("admin") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("admin"));
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("foreman") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("foreman"));
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("client") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("client"));*/
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("user") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("user"));
							//v1.7 Ends
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("property") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("property"));
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("inventory") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("inventory"));
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("backOfficeReports") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("backOfficeReports"));
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("preferences") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("preferences"));
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("toggles") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("toggles"));
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("myAccount") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("myAccount"));
							//v1.7 Starts
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("workOrderRequests") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("workOrderRequests"));
							//v1.7 Ends
							
							//v1.14 Starts
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("dashboard") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("dashboard"));
							//v1.14 Ends
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("invoice") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("invoice"));
							
							functionalTogglesCtrlMasterValues.put(functionalKey+(resultSet.getBoolean("paymentOption") == true ?"":WordUtils.capitalize(moduleNames.get(++count))), resultSet.getBoolean("paymentOption"));
						}else {
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("workOrder"));
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("proposal"));
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("asset"));
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("irrigation"));
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("maintenance"));
							//v1.7 Starts
							/*functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("admin"));
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("foreman"));
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("client"));*/
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("user"));
							//v1.7 Ends
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("property"));
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("inventory"));
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("backOfficeReports"));
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("preferences"));
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("toggles"));
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("myAccount"));
							//v1.7 Starts
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("workOrderRequests"));
							//v1.7 Ends
							
							//v1.14 Starts
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("dashboard"));
							//v1.14 Ends
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("invoice"));
							
							functionalTogglesCtrlMasterValues.put(functionalKey+WordUtils.capitalize(moduleNames.get(++count)), resultSet.getBoolean("paymentOption"));
						}
						count = 0;
					}
					
					
					modAccsFuncToggsMasterCtrlDTO.setModuleNames(moduleNames);
					modAccsFuncToggsMasterCtrlDTO.setModuleAccessCtrlDescAndHelpText(moduleAccessCtrlDescAndHelpText);
					modAccsFuncToggsMasterCtrlDTO.setFunctionalToggleCtrlDescAndHelpText(functionalToggleCtrlDescAndHelpText);
					modAccsFuncToggsMasterCtrlDTO.setModuleAccessCtrlMasterValues(moduleAccessCtrlMasterValues);
					modAccsFuncToggsMasterCtrlDTO.setFunctionalTogglesCtrlMasterValues(functionalTogglesCtrlMasterValues);
					
				}else {
					logger.error("Connection is null or closed in getModuleAccessControlsFromMaster()");
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Exception occured in getModuleAccessControlsFromMaster() -> "+e.getMessage()); 
			} finally {
				close(resultSet);
				close(preparedStatement);
				close(conn);
			}

		return modAccsFuncToggsMasterCtrlDTO;
		}
	
	public ForemanToggleControlsDTO getForemanActiveToggles(UserCacheKey userCacheKey) {

		// logger.info("Fetching whether the foreman is active or not method is
		// invoked");

		int count = 0;

		String moduleToggles = null;
		String functionalToggles = null;

		DBConnector db2Connector = DBConnector.getInstance();
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		ForemanToggleControlsDTO foremanToggleControlsDTO = null;

		List<String> activeModuleAccessToggles = null;
		List<String> activeFunctionalToggles = null;
		try {
			conn = db2Connector.getConnection(true);
			if (conn != null && !conn.isClosed()) {
				
				foremanToggleControlsDTO = new ForemanToggleControlsDTO();

				String	statement = " select moduleToggles, functionalToggles from ff_foreman_module_fun_toggles where foremanId = ? and role = ?";

				preparedStatement = conn.prepareStatement(statement);
				preparedStatement.setInt(++count, userCacheKey.getUserId());
				preparedStatement.setString(++count, userCacheKey.getUserRole());

				resultSet = preparedStatement.executeQuery();

				if (resultSet.next()) {

					foremanToggleControlsDTO = new ForemanToggleControlsDTO();
					moduleToggles = resultSet.getString("moduleToggles").replace("[", "").replace("]", "")
							.replaceAll("\\s+", "");
					functionalToggles = resultSet.getString("functionalToggles").replace("[", "").replace("]", "")
							.replaceAll("\\s+", "");

					activeModuleAccessToggles = new ArrayList<String>(Arrays.asList(moduleToggles.split(",")));

					activeFunctionalToggles = new ArrayList<String>(Arrays.asList(functionalToggles.split(",")));
					
					foremanToggleControlsDTO.setForemanActiveModAccessTogglesList(activeModuleAccessToggles);
					foremanToggleControlsDTO.setForemanActiveFunctionalTogglesList(activeFunctionalToggles);

					foremanToggleControlsDTO.setArrForemanModuleAccessToggles(
							activeModuleAccessToggles.toArray(new String[activeModuleAccessToggles.size()]));

					foremanToggleControlsDTO.setArrForemanFunctionalToggles(
							activeFunctionalToggles.toArray(new String[activeModuleAccessToggles.size()]));

				}

			} else {
				logger.error("Connection is null or closed in getForemanActiveModuleAccsAndFunToggles()");
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occured in getForemanActiveModuleAccsAndFunToggles() -> " + e.getMessage());
		} finally {
			close(resultSet);
			close(preparedStatement);
			close(conn);
		}

		return foremanToggleControlsDTO;
	}
	
	public List<Object> checkUserLogin(LoginBean bean, String companyId,
			String userRole, int userId) {

		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<Object> retList = new ArrayList<Object>();
		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "SELECT usrId,usrName,usrRole,usrEmail,usrPhoneNumber,usrComId,usrRole,comUpdatedTime,"
						+ "comActiveStatus,isGracePeriod,gracePeriodDate,length(comLogo) as companyLogoLength,comCustomerType,comCreatedTime "
						+ "FROM ff_user_management left join ff_company_management on usrComId=comId "
						+ "WHERE usrRowStatus IS NULL and comRowStatus is null and usrRole=? and usrComId=? and usrId = ?";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setString(1, userRole);
				prepStmnt.setString(2, companyId);
				prepStmnt.setInt(3, userId);
				
				logger.info("User role in after updating password ->"+userRole);
				logger.info("Company Id in after updating password ->"+companyId);
				logger.info("User id in after updating password ->"+userId);
				
				resInfo = prepStmnt.executeQuery();
				if (resInfo.next()) {
					retList.add(getString(resInfo, "usrId", "0"));
					retList.add(getString(resInfo, "usrName", ""));
					retList.add(getString(resInfo, "usrRole", ""));
					retList.add(getString(resInfo, "usrEmail", ""));
					retList.add(getString(resInfo, "usrPhoneNumber", ""));

					retList.add(getString(resInfo, "usrComId", "0"));

					if (resInfo.getString("usrRole").equals("Cappsure Admin")) {

						retList.add("./resources/assets/img/logo.png");
					} else {

						String companyLogoString = "no-image";
						if (getInt(resInfo, "companyLogoLength", 0) >= 2) {

							long updateTime = new Date().getTime();
							try {
								if (resInfo.getTimestamp("comUpdatedTime") != null) {
									updateTime = resInfo.getTimestamp(
											"comUpdatedTime").getTime();
								}
							} catch (Exception e) {
								logger.error("Exception in get updatetime : "
										+ e);
							}

							companyLogoString = S3_BUCKET_HOME+COMPANY_S3_URL + "/"
									+ getString(resInfo, "usrComId", "0")
									+ ".png?upTime=" + updateTime;

						}
						retList.add(companyLogoString);
					}

					if (getString(resInfo, "usrRole", "").equals(
							"Cappsure Admin")) {
						retList.add("1");
					} else {
						retList.add(resInfo.getString("comActiveStatus"));
					}
					
					String customerType = getString(resInfo, "comCustomerType", "");
					
					retList.add(customerType);
					
					if(customerType.equalsIgnoreCase(CommonVariables.FREE_TRIAL_USER)) {
						if(CommonDAO.isWalkMeCompany(new Integer(getString(resInfo, "usrComId", "0")))) {
							retList.add(getNumberofDaysDiff(
										CommonDAO.getWalkMeCompanyCraetedTime(),false));
						} else {
							retList.add(getNumberofDaysDiff(resInfo.getTimestamp("comCreatedTime"),false));
						}
					} else {
						retList.add("-1");
					}
					
					retList.add(getNumberofDaysDiff(resInfo.getTimestamp("comCreatedTime"),true));
				}
			} catch (SQLException scon) {
				logger.error("Login SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Login Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);
		return retList;
	}
	
	public HashMap<String, List<String>> getUserTogglesByRole(Integer userRole){
		
	//logger.info("Fetching whether the foreman is active or not method is invoked");
		
	int count = 0;
	
	String userDefaultModuleAccessToggles = null;
	String userDefaultFunctionalToggles = null;
	
	DBConnector db2Connector = DBConnector.getInstance();
	Connection conn = null;
	PreparedStatement preparedStatement = null;
	ResultSet resultSet = null;
	
	List<String> userDefaultModAccsTogglesList = null;
	List<String> userDefaultFuncTogglesList = null;
	
	HashMap<String, List<String>> userDefaultTogglesMap = null;
		try {
			conn = db2Connector.getConnection(true);
			if(conn!=null && !conn.isClosed()) {
				
				String statement = " select moduleToggles, functionalToggles from ff_user_default_toggles where userRole = ?";
				
				preparedStatement = conn.prepareStatement(statement);
				preparedStatement.setInt(++count, userRole);
				
				resultSet = preparedStatement.executeQuery();
				
				if(resultSet.next()) {
					userDefaultTogglesMap = new HashMap<String, List<String>>();
					userDefaultModuleAccessToggles = resultSet.getString("moduleToggles").replaceAll("\\s+","");
					userDefaultFunctionalToggles = resultSet.getString("functionalToggles").replaceAll("\\s+","");
					
					userDefaultModAccsTogglesList = new ArrayList<String>( Arrays.asList(userDefaultModuleAccessToggles.split(",")));
					userDefaultFuncTogglesList = new ArrayList<String>( Arrays.asList(userDefaultFunctionalToggles.split(",")));
					
					userDefaultTogglesMap.put("moduleAccessToggles", userDefaultModAccsTogglesList);
					userDefaultTogglesMap.put("functionalToggles", userDefaultFuncTogglesList);
					
				}
				
			}else {
				logger.error("Connection is null or closed in getForemanActiveModuleAccsAndFunToggles()");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occured in getForemanActiveModuleAccsAndFunToggles() -> "+e.getMessage()); 
		} finally {
			close(resultSet);
			close(preparedStatement);
			close(conn);
		}

	return userDefaultTogglesMap;
	}
	
	
	public LoginBean getSuperAdminDetails(LoginBean loginBean, int companyId,
			String userRole){
		
	//logger.info("Fetching whether the foreman is active or not method is invoked");
		
	int count = 0;
	
	
	DBConnector db2Connector = DBConnector.getInstance();
	Connection conn = null;
	PreparedStatement preparedStatement = null;
	ResultSet resultSet = null;
	
	
		try {
			conn = db2Connector.getConnection(true);
			if(conn!=null && !conn.isClosed()) {
				
				String statement = " select usrId from ff_user_management WHERE usrRowStatus IS NULL and usrRole=? and usrComId=?";
				
				preparedStatement = conn.prepareStatement(statement);
				preparedStatement.setString(++count, userRole);
				preparedStatement.setInt(++count, companyId);
				
				resultSet = preparedStatement.executeQuery();
				
				if(resultSet.next()) {
					loginBean.setUserId(resultSet.getInt("usrId"));
				}
				
			}else {
				logger.error("Connection is null or closed in getForemanActiveModuleAccsAndFunToggles()");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occured in getForemanActiveModuleAccsAndFunToggles() -> "+e.getMessage()); 
		} finally {
			close(resultSet);
			close(preparedStatement);
			close(conn);
		}

	return loginBean;
	}
	
	public void insertFacesFilterMapping(CappsurePreferencesBean cappsurePreferencesBean){
		
	//logger.info("Fetching whether the foreman is active or not method is invoked");
		
	int count = 0;
	
	
	DBConnector db2Connector = DBConnector.getInstance();
	Connection conn = null;
	PreparedStatement preparedStatement = null;
	
	
		try {
			conn = db2Connector.getConnection(true);
			if(conn!=null && !conn.isClosed()) {
				
				String statement = "insert into ff_faces_filter_mapping (filterKey, listPageName, functionalPageName, createdBy, createdTime) values (?, ?, ?, ?, now())";
				
				preparedStatement = conn.prepareStatement(statement);
				preparedStatement.setString(++count, cappsurePreferencesBean.getFilterKey());
				preparedStatement.setString(++count, cappsurePreferencesBean.getListPageName());
				preparedStatement.setString(++count, cappsurePreferencesBean.getFunctionalPageName());
				preparedStatement.setInt(++count, cappsurePreferencesBean.getLoginBean().getUserId());
				
				int insertResult = preparedStatement.executeUpdate();
				
				if(insertResult>0) {
					logger.info("Successfully inserted the new faces filter mapping");
				}
				
			}else {
				logger.error("Connection is null or closed in insertFacesFilterMapping()");
			}
			
		} catch (Exception e) {
			logger.error("Exception occured in insertFacesFilterMapping() -> "+e); 
		} finally {
			close(preparedStatement);
			close(conn);
		}

	}
	
	public void updateFacesFilterMapping(FacesFilterMappingDTO facesFilterMappingDTO, int userId){
		
	//logger.info("Fetching whether the foreman is active or not method is invoked");
		
	int count = 0;
	
	
	DBConnector db2Connector = DBConnector.getInstance();
	Connection conn = null;
	PreparedStatement preparedStatement = null;
	
	
		try {
			conn = db2Connector.getConnection(true);
			if(conn!=null && !conn.isClosed() && facesFilterMappingDTO!= null) {
				
				String statement = "update ff_faces_filter_mapping set filterKey = ?, listPageName = ?, functionalPageName = ?, updatedBy = ?, createdTime = now() where recordNo = ?";
				
				preparedStatement = conn.prepareStatement(statement);
				preparedStatement.setString(++count, facesFilterMappingDTO.getFilterKey());
				preparedStatement.setString(++count, facesFilterMappingDTO.getListPageName());
				preparedStatement.setString(++count, facesFilterMappingDTO.getFunctionalPageName());
				preparedStatement.setInt(++count, userId);
				preparedStatement.setInt(++count, facesFilterMappingDTO.getFacesFilterMappingId());
				
				int updateResult = preparedStatement.executeUpdate();
				
				if(updateResult>0) {
					logger.info("Successfully updated the new faces filter mapping");
				}
				
			}else {
				logger.error("Connection is null or closed. Else Faces filter mapping object is empty in updateFacesFilterMapping()");
			}
			
		} catch (Exception e) {
			logger.error("Exception occured in updateFacesFilterMapping() and error message is -> "+e); 
		} finally {
			close(preparedStatement);
			close(conn);
		}

	}
	
	/**
	 * This method is used to maintain the foreman role
	 * and it will find whether the mapping is alerady exist
	 * If not it will insert the new record
	 * Otherwise it will update
	 * and method getForemanRoleMappingDetailsis used to
	 * find whether the mapping is exist
	 * @param cappsurePreferencesBean
	 */
	public void maintainForemanRoleMapping(CappsurePreferencesBean cappsurePreferencesBean){
		
	int count = 0;
	
	String query =  null;
	
	DBConnector db2Connector = DBConnector.getInstance();
	Connection conn = null;
	PreparedStatement preparedStatement = null;
	
	
		try {
			conn = db2Connector.getConnection(true);
			if(conn!=null && !conn.isClosed()) {
				
				String[] foremanRole = getForemanRoleMappingDetails(cappsurePreferencesBean.getForemanRoleId());
				
				if(foremanRole == null) {
					query = "insert into ff_foreman_role_mapping (role, mappedForemanRoles, createdBy, createdTime) values (?, ?, 1, now())";
					
					preparedStatement = conn.prepareStatement(query);
					preparedStatement.setInt(++count, cappsurePreferencesBean.getForemanRoleId());
					preparedStatement.setString(++count, CommonUtil.convertObjectArrayToString(cappsurePreferencesBean.getMappedForemanRole(), ","));
					
				}else {
					query = "update ff_foreman_role_mapping set mappedForemanRoles = ?, updatedBy = 1, updatedTime = now() where role = ?";
					
					preparedStatement = conn.prepareStatement(query);
					preparedStatement.setString(++count, CommonUtil.convertObjectArrayToString(cappsurePreferencesBean.getMappedForemanRole(), ","));
					preparedStatement.setInt(++count, cappsurePreferencesBean.getForemanRoleId());
					
				}
				
				int insertResult = preparedStatement.executeUpdate();
				
				if(insertResult>0) {
					logger.info("Successfully maintained the foreman role mapping");
				}
				
			}else {
				logger.error("Connection is null or closed in maintainForemanRoleMapping()");
			}
			
		} catch (Exception e) {
			logger.error("Exception occured in maintainForemanRoleMapping() -> ", e); 
		} finally {
			close(preparedStatement);
			close(conn);
		}

	}
	
	public List<FacesFilterMappingDTO> getFacesFilterMappingDetails(){
		
	DBConnector db2Connector = DBConnector.getInstance();
	Connection conn = null;
	PreparedStatement preparedStatement = null;
	ResultSet resultSet = null;
	
	FacesFilterMappingDTO facesFilterMappingDTO = null;
	List<FacesFilterMappingDTO> listFacesFilterMappingDTO = null;
	
		try {
			conn = db2Connector.getConnection(true);
			if(conn!=null && !conn.isClosed()) {
				listFacesFilterMappingDTO = new ArrayList<FacesFilterMappingDTO>();
				
				String statement = " select recordNo, filterKey, listPageName, functionalPageName from ff_faces_filter_mapping";
				
				preparedStatement = conn.prepareStatement(statement);
				
				resultSet = preparedStatement.executeQuery();
				
				while(resultSet.next()) {
					facesFilterMappingDTO = new FacesFilterMappingDTO();
					facesFilterMappingDTO.setFacesFilterMappingId(resultSet.getInt("recordNo"));
					facesFilterMappingDTO.setFilterKey(resultSet.getString("filterKey"));
					facesFilterMappingDTO.setListPageName(resultSet.getString("listPageName"));
					facesFilterMappingDTO.setFunctionalPageName(resultSet.getString("functionalPageName"));
					listFacesFilterMappingDTO.add(facesFilterMappingDTO);
				}
				
			}else {
				logger.error("Connection is null or closed in getForemanActiveModuleAccsAndFunToggles()");
			}
			
		} catch (Exception e) {
			logger.error("Exception occured in getForemanActiveModuleAccsAndFunToggles() -> "+e.getMessage()); 
		} finally {
			close(resultSet);
			close(preparedStatement);
			close(conn);
		}

	return listFacesFilterMappingDTO;
	}
	
	
	/**
	 * This method is used to find
	 * whether the role mapping is exist or not
	 * 
	 * @param foremanRoleIdToLoad
	 * @return
	 */
	public String[] getForemanRoleMappingDetails(int foremanRoleIdToLoad) {

		int count = 0;

		String[] foremanRoleMappedArr = null;

		DBConnector db2Connector = DBConnector.getInstance();
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			conn = db2Connector.getConnection(true);
			if (conn != null && !conn.isClosed()) {

				String statement = " select role, mappedForemanRoles from ff_foreman_role_mapping where role = ?";

				preparedStatement = conn.prepareStatement(statement);

				preparedStatement.setInt(++count, foremanRoleIdToLoad);

				resultSet = preparedStatement.executeQuery();

				if (resultSet.next()) {
					foremanRoleMappedArr = resultSet.getString("mappedForemanRoles").split(",");
				}

			} else {
				logger.error("Connection is null or closed in getForemanRoleMappingDetails()");
			}

		} catch (Exception e) {
			logger.error("Exception occured in getForemanRoleMappingDetails() -> " + e.getMessage());
		} finally {
			close(resultSet);
			close(preparedStatement);
			close(conn);
		}

		return foremanRoleMappedArr;
	}
	
	public boolean checkDefaultTogglesIsExist(int userRole){
		
	int count =0;	
	
	boolean isDefaultToggleExist = false;	
		
	DBConnector db2Connector = DBConnector.getInstance();
	Connection conn = null;
	PreparedStatement preparedStatement = null;
	ResultSet resultSet = null;
	
	
		try {
			conn = db2Connector.getConnection(true);
			if(conn!=null && !conn.isClosed()) {
				
				String statement = " select userRole from ff_user_default_toggles where userRole = ? ";
				
				preparedStatement = conn.prepareStatement(statement);
				preparedStatement.setInt(++count, userRole);
				
				resultSet = preparedStatement.executeQuery();
				
				if(resultSet.next()) {
					isDefaultToggleExist = true;
				}
				
			}else {
				logger.error("Connection is null or closed in getForemanActiveModuleAccsAndFunToggles()");
			}
			
		} catch (Exception e) {
			logger.error("Exception occured in getForemanActiveModuleAccsAndFunToggles() -> "+e.getMessage()); 
		} finally {
			close(resultSet);
			close(preparedStatement);
			close(conn);
		}

	return isDefaultToggleExist;
	}
	
	public void maintainForemanDefaultToggles(CappsurePreferencesBean cappsurePreferencesBean){
		
	//logger.info("Fetching whether the foreman is active or not method is invoked");
	int count = 0;
	
	boolean isDefaultTogglesExist = false;
	
	
	DBConnector db2Connector = DBConnector.getInstance();
	Connection conn = null;
	PreparedStatement preparedStatement = null;
	
	logger.info("cappsurePreferencesBean user role id is:"+cappsurePreferencesBean.getUserRoleId());
	
		try {
			conn = db2Connector.getConnection(true);
			if(conn!=null && !conn.isClosed() && cappsurePreferencesBean!= null) {
				
				isDefaultTogglesExist = checkDefaultTogglesIsExist(cappsurePreferencesBean.getUserRoleId());
				String queryToMaintainDefaultToggles = isDefaultTogglesExist ? "update ff_user_default_toggles set moduleToggles = ?, functionalToggles = ?, updatedBy = ?, updatedTime = now() where userRole = ?"
						:" insert into ff_user_default_toggles (userRole, moduleToggles, functionalToggles, createdBy, createdTime) values (?, ?, ?, ?, now())";
				
				preparedStatement = conn.prepareStatement(queryToMaintainDefaultToggles);
				
				if(isDefaultTogglesExist) {
					preparedStatement.setString(++count, Arrays.asList(cappsurePreferencesBean.getForemanToggleControlsDTO().getArrForemanModuleAccessToggles()).toString().replace("[", "").replace("]", ""));
					preparedStatement.setString(++count, Arrays.asList(cappsurePreferencesBean.getForemanToggleControlsDTO().getArrForemanFunctionalToggles()).toString().replace("[", "").replace("]", ""));
					preparedStatement.setInt(++count, cappsurePreferencesBean.getLoginBean().getUserId());
					preparedStatement.setInt(++count, cappsurePreferencesBean.getUserRoleId());
				}else {
					preparedStatement.setInt(++count, cappsurePreferencesBean.getUserRoleId());
					preparedStatement.setString(++count, Arrays.asList(cappsurePreferencesBean.getForemanToggleControlsDTO().getArrForemanModuleAccessToggles()).toString().replace("[", "").replace("]", ""));
					preparedStatement.setString(++count, Arrays.asList(cappsurePreferencesBean.getForemanToggleControlsDTO().getArrForemanFunctionalToggles()).toString().replace("[", "").replace("]", ""));
					preparedStatement.setInt(++count, cappsurePreferencesBean.getLoginBean().getUserId());
				}
				
				int result = preparedStatement.executeUpdate();
				
				if(result>0) {
					logger.info("Successfully maintained the user default toggle for following user role ->"+cappsurePreferencesBean.getUserRoleId());
				}
				
			}else {
				logger.error("Connection is null or closed. Else Cappsure preference bean is empty in maintainForemanDefaultToggles()");
			}
			
		} catch (Exception e) {
			logger.error("Exception occured in maintainForemanDefaultToggles() and error message is -> "+e); 
		} finally {
			close(preparedStatement);
			close(conn);
		}

	}
	//v1.6 Ends
	
	public Integer getUserRoleIdByRoleType(String roleType) {
		
		Integer result = 0;

		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		PreparedStatement prepStmnt = null;
		Connection con = null;
		ResultSet resInfo = null;

		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "SELECT * FROM ff_role WHERE  roleName = ? and roleType = ?";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setString(1, StringUtils.upperCase(roleType));
				prepStmnt.setString(2, StringUtils.upperCase(CommonVariables.FOREMAN));
				resInfo = prepStmnt.executeQuery();
				

				if (resInfo != null && resInfo.next()) {
					result = getInt(resInfo, "roleId", 0);
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);

		return result;
	}
	
	public String getUserRoleByRoleId(String roleId) {
		
		String result = "";

		DBConnector db2Connector = DBConnector.getInstance();
		boolean retry;
		int numOfretry = 0;
		PreparedStatement prepStmnt = null;
		Connection con = null;
		ResultSet resInfo = null;

		do {
			retry = false;
			try {
				con = db2Connector.getConnection(true);

				String selectStatement = "SELECT * FROM ff_role WHERE  roleId = ?";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setString(1, roleId);
				resInfo = prepStmnt.executeQuery();
				

				if (resInfo != null && resInfo.next()) {
					result = getString(resInfo, "roleName", "");
				}
			} catch (SQLException scon) {
				logger.error("Select SQLException.." + scon + "  " + numOfretry);
				if (numOfretry < 2) {
					numOfretry++;
					try {
						close(con);
					} catch (Exception e) {
					}
					con = db2Connector.getConnection(true);
					retry = true;
				} else {
					retry = false;
					logger.error("Select Exception :" + scon.getMessage());
				}
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		} while (retry);

		return result;
	}
	
	//v1.9 Starts
	public List<UserDTO> getForemanCompanyDetails(String userEmail){
		
	int count =0;	
	
	DBConnector db2Connector = DBConnector.getInstance();
	Connection conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	
	UserDTO userDTO = null;
	
	List<UserDTO> foremanAndCompanyDetails = new ArrayList<UserDTO>();
		try {
			conn = db2Connector.getConnection(true);
			if(conn!=null && !conn.isClosed()) {
				
				/*String getForemanCompanies = " select userManagement.usrId, userManagement.usrComId, companyManagement.comName " + 
						" from ff_user_management userManagement, ff_company_management companyManagement " + 
						" where userManagement.usrRowStatus is null " + 
						" and companyManagement.comRowStatus is null " + 
						" and userManagement.usrComId = companyManagement.comId " + 
						" and userManagement.usrEmail = ? ";*/
				
				String getUserCompanies = "SELECT userManagement.usrid, " + 
									" userManagement.usrcomid, " + 
									" companyManagement.comname, " + 
									" userManagement.usrrole " + 
									" FROM   ff_user_management userManagement " + 
									" LEFT JOIN ff_company_management companyManagement " + 
									" ON userManagement.usrcomid = companyManagement.comid " + 
									" WHERE  userManagement.usrrowstatus IS NULL " + 
									" AND companyManagement.comrowstatus IS NULL " + 
									" AND userManagement.usremail = ? ";
				
				ps = conn.prepareStatement(getUserCompanies);
				ps.setString(++count, userEmail);
				
				rs = ps.executeQuery();
				
				while(rs.next()) {
					userDTO = new UserDTO();
					userDTO.setUserId(rs.getInt("usrId"));
					userDTO.setCompanyId(rs.getInt("usrComId"));
					userDTO.setCompanyName(getString(rs, "comName", "All Company"));
					userDTO.setUserRole(rs.getString("usrrole"));
					foremanAndCompanyDetails.add(userDTO);
				}
				
			}else {
				logger.error("Connection is null or closed in getForemanActiveModuleAccsAndFunToggles()");
			}
			
		} catch (Exception e) {
			logger.error("Exception occured in getForemanActiveModuleAccsAndFunToggles() -> "+e.getMessage()); 
		} finally {
			close(rs);
			close(ps);
			close(conn);
		}

	return foremanAndCompanyDetails;
	}
	
	public List<UserDTO> getClientCompanyDetails(String userEmail){
		
		int count =0;	
		
		DBConnector db2Connector = DBConnector.getInstance();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		UserDTO userDTO = null;
		
		List<UserDTO> clientAndCompanyDetails = new ArrayList<UserDTO>();
			try {
				conn = db2Connector.getConnection(true);
				if(conn!=null && !conn.isClosed()) {
					
					String getClientCompanies = " select clientId, comId, comName from ff_client client, " + 
							" ff_client_company clientCompany, " + 
							" ff_company_management companyManagement " + 
							" where clientRowStatus is null " + 
							" and companyManagement.comRowStatus is null " + 
							" and companyManagement.comId = clientCompany.ccComId " + 
							" and client.clientId = clientCompany.ccClientId " + 
							" and client.clientEmail = ? ";
					
					ps = conn.prepareStatement(getClientCompanies);
					ps.setString(++count, userEmail);
					
					rs = ps.executeQuery();
					
					while(rs.next()) {
						userDTO = new UserDTO();
						userDTO.setUserId(rs.getInt("clientId"));
						userDTO.setCompanyId(rs.getInt("comId"));
						userDTO.setCompanyName(rs.getString("comName"));
						userDTO.setUserRole(CommonVariables.CLIENT);
						clientAndCompanyDetails.add(userDTO);
					}
					
				}else {
					logger.error("Connection is null or closed in getForemanActiveModuleAccsAndFunToggles()");
				}
				
			} catch (Exception e) {
				logger.error("Exception occured in getForemanActiveModuleAccsAndFunToggles() -> "+e.getMessage()); 
			} finally {
				close(rs);
				close(ps);
				close(conn);
			}

		return clientAndCompanyDetails;
		}
	//v1.9 Ends
	
	public List<SelectItem> loadBillableContactList(int companyId) {
		DBConnector db2Connector = DBConnector.getInstance();
		
		Connection con = null;
		PreparedStatement prepStmnt = null;
		ResultSet resInfo = null;
		List<SelectItem> clientList = new ArrayList<SelectItem>();
			try {
				con = db2Connector.getConnection(true);
				String selectStatement = "Select distinct clientId, clientName from " + 
						" ff_client_company , ff_client, ff_client_management " + 
						" where ccComId= ? and clientRowStatus is null " + 
						" and ccClientId=clientId " + 
						" and cmRowStatus is null " + 
						" and cmBillingClientId = clientId " + 
						" order by clientName ";

				prepStmnt = con.prepareStatement(selectStatement);
				prepStmnt.setInt(1, companyId);
				resInfo = prepStmnt.executeQuery();
				while (resInfo.next()) {
					clientList.add(new SelectItem(""
							+ resInfo.getInt("clientId"), getString(resInfo,
							"clientName", "")));
				}
			} catch (Exception e) {
				logger.error("Exception occured at method getting billable contact list and exception is ->", e);
			} finally {
				close(resInfo);
				close(prepStmnt);
				close(con);
			}
		return clientList;
	}
	
	// v1.18 Starts Here
		/**
		* 
		* This method gets all the companies which have their billing date approaching in the next 7 days 
		* for which the billing price has not been set by the administrator.
		* 
		* @return List<CompanyDTO>
		*/

		public List<CompanyDTO> getBillingPriceDetails() {

			int count = 0;

			List<CompanyDTO> paymentList = null;

			CompanyDTO companyDTO = null;

			DBConnector db2Connector = DBConnector.getInstance();
			Connection conn = null;
			PreparedStatement preparedStatement = null;
			ResultSet resultSet = null;
			try {
				conn = db2Connector.getConnection(true);
				if (conn != null && !conn.isClosed()) {

					paymentList = new ArrayList<CompanyDTO>();

					/*This query used to selecting the companies where company status should be active and company billing amount should be 0.00*/
					String statement = "select comName,comId,comBillingDate from ff_company_management where comRowStatus is null and isAutoBilling = 0 and comActiveStatus = ? "
							+ " AND (comBillingAmount=0.00 or comBillingAmount=null or comBillingAmount=0) and comRecurringBill=?";

					preparedStatement = conn.prepareStatement(statement);
					preparedStatement.setInt(++count, CommonVariables.TRUE);
					preparedStatement.setInt(++count, CommonVariables.TRUE);
					resultSet = preparedStatement.executeQuery();

					
					
					while (resultSet.next()) {
						logger.info("Company Name"+resultSet.getString(CommonVariables.COMPANYNAME));
						logger.info("Company Id"+resultSet.getInt(CommonVariables.COMPANYID));
						logger.info("Company Billing Date"+resultSet.getTimestamp(CommonVariables.COMPANYBILLINGDATE));
						
						companyDTO = new CompanyDTO(1,
								resultSet.getString(CommonVariables.COMPANYNAME),
								resultSet.getInt(CommonVariables.COMPANYID),
								resultSet.getTimestamp(CommonVariables.COMPANYBILLINGDATE)

						);

						paymentList.add(companyDTO);

					}

					logger.info("paymentList size :" + paymentList.size());
				} else {
					logger.error("Connection is null or closed in getBillingPriceDetails()");
				}

			} catch (Exception e) {
				logger.error("Exception occured in getBillingPriceDetails() -> " + e);

			} finally {
				close(resultSet);
				close(preparedStatement);
				close(conn);
			}

			return paymentList;
		}
		// v1.18 Ends Here	
}
