package com.moxieit.orderplatform.lambda.function.service.impl;

import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;
import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.Page;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.ScanExpressionSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.moxieit.orderplatform.DB.DBService;
import com.moxieit.orderplatform.function.service.api.GoogleDTO;
import com.moxieit.orderplatform.function.service.api.GoogleService;
import com.moxieit.orderplatform.lambda.response.BaseResponse;
import com.moxieit.orderplatform.lambda.response.GoogleResponse;


public class GooglePickUpServiceImpl implements GoogleService{

	@Override
	public BaseResponse serveLex(GoogleDTO googleDTO, Context context) {
		// TODO Auto-generated method stub
		DynamoDB dynamoDB = DBService.getDBConnection();
		Table orderItemTable = dynamoDB.getTable("OrderItems");
		Table orderTable = dynamoDB.getTable("Order");
		
		List<Date> dates = new ArrayList<Date>();
		Calendar calendar = Calendar.getInstance();
		
		ScanExpressionSpec xspec1 = new ExpressionSpecBuilder().withCondition(S("userId").eq(googleDTO.getUserId())
				.and(N("creationDate").ge(System.currentTimeMillis() - 900000)).and(S("itemQuantityAdd").eq("true")))
				.buildForScan();
		
		ItemCollection<ScanOutcome> scan1 = orderItemTable.scan(xspec1);

		Consumer<Item> action1 = new Consumer<Item>() {
			public void accept(Item t1) {
			
			Number creationDate = t1.getNumber("creationDate");
			String x = creationDate.toString();				
			long milliSeconds = Long.parseLong(x);					
			calendar.setTimeInMillis(milliSeconds);		
			Date recentDate = null;		
				//recentDate = (Date) formatter1.parse(orderDate);
				recentDate = (Date) calendar.getTime();				
						
			dates.add(recentDate);
		

			}

		};
		scan1.forEach(action1);
		Date latest = Collections.max(dates);	
		long itemdateMilliSec = latest.getTime();
		System.out.println("itemdateMilliSec date :"+itemdateMilliSec);
		
		 ScanExpressionSpec xspec2 = new ExpressionSpecBuilder().withCondition(S("userId").eq(googleDTO.getUserId())
					.and(N("creationDate").ge(System.currentTimeMillis() - 900000)).and(S("itemQuantityAdd").eq("true"))
					.and(N("creationDate").eq((Number)itemdateMilliSec)))
					.buildForScan();

			ItemCollection<ScanOutcome> scan2 = orderItemTable.scan(xspec2);
			Item order1 = null;        
			Page<Item, ScanOutcome> firstPage1 = scan2.firstPage();	
			
			if (firstPage1.iterator().hasNext()) {
				order1 = firstPage1.iterator().next();
     	 UpdateItemSpec updateItemSpec1 = new UpdateItemSpec().withPrimaryKey("uuid", order1.getString("orderuuid"))
					.withUpdateExpression("set pickUp = :val,address = :add")					
					.withValueMap(new ValueMap().withString(":val", "30 Mins").withString(":add", "NULL"));					
			UpdateItemOutcome outcome1 = orderTable.updateItem(updateItemSpec1);
			outcome1.getItem();
			GoogleResponse googleResponse = new GoogleResponse();
			googleResponse.setSpeech("Please provide your phone number for this order and collect you order within 30 minutes.");
			return googleResponse;
					
			}
		return null;
	}
	

}
