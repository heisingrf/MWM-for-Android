                                                                     
                                                                     
                                                                     
                                             
 /*****************************************************************************
  *  Copyright (c) 2011 Meta Watch Ltd.                                       *
  *  www.MetaWatch.org                                                        *
  *                                                                           *
  =============================================================================
  *                                                                           *
  *  Licensed under the Apache License, Version 2.0 (the "License");          *
  *  you may not use this file except in compliance with the License.         *
  *  You may obtain a copy of the License at                                  *
  *                                                                           *
  *    http://www.apache.org/licenses/LICENSE-2.0                             *
  *                                                                           *
  *  Unless required by applicable law or agreed to in writing, software      *
  *  distributed under the License is distributed on an "AS IS" BASIS,        *
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
  *  See the License for the specific language governing permissions and      *
  *  limitations under the License.                                           *
  *                                                                           *
  *****************************************************************************/

 /*****************************************************************************
  * Call.java                                                                 *
  * Call                                                                      *
  * Call watch mode                                                           *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WatchType;
import org.metawatch.manager.Notification.VibratePattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

public class Call {
	
	public static boolean isRinging = false;
	public static boolean inCall = false;
	public static String phoneNumber = null;
	public static boolean previousSpeakerphoneState = false;
	public static int previousRingerMode = -1;
	public static boolean voicemailWaiting = false;
	
	final static byte CALL_ANSWER = 90;
	final static byte CALL_DISMISS = 91;
	final static byte CALL_MENU = 92;
	
	public static void startRinging(Context context, String number) {
		toCall();
		
		isRinging = true;
		
		String name = Utils.getContactNameFromNumber(context, number);	

		if (MetaWatchService.watchType == WatchType.DIGITAL) {
		
			Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);
			
			TextPaint paintSmall = new TextPaint();
			paintSmall.setColor(Color.BLACK);
			paintSmall.setTextSize(FontCache.instance(context).Large.size);
			paintSmall.setTypeface(FontCache.instance(context).Small.face);
			
			canvas.drawColor(Color.WHITE);
			
			int top = 0;
			int centre = 48;
			
			Bitmap contactImage = Utils.getContactPhotoFromNumber(context, number);
			if(contactImage!=null) {
				int iconSize = 70;
				contactImage = Utils.resize(contactImage,iconSize,iconSize);
				contactImage = Utils.ditherTo1bit(contactImage, Preferences.invertLCD);
				canvas.drawBitmap(contactImage,(96-iconSize)/2,0,null);
				
				top = iconSize;
				centre = (96+iconSize)/2;
			}
			else {
				canvas.drawBitmap(Utils.getBitmap(context, "phone.bmp"), 0, 0, null);
			}
					
			String displayText;
			if (name.equals(number))
				displayText = number;
			else
				displayText = name+"\n\n"+number;
			
			
			canvas.save();			
			StaticLayout layout = new StaticLayout(displayText, paintSmall, 96, Layout.Alignment.ALIGN_CENTER, 1.0f, 0, false);
			int height = layout.getHeight();
			int textY = centre - (height/2);
			if(textY<top) {
				textY=top;
			}
			canvas.translate(0, textY); //position the text
			canvas.clipRect(0,0,96,35);
			layout.draw(canvas);
			canvas.restore();
			
			// Draw icons			
			canvas.drawBitmap(Utils.getBitmap(context, "action_up.bmp"), 87, -2, null);
			canvas.drawBitmap(Utils.getBitmap(context, "action_reject.bmp"), 87, 43, null);
			canvas.drawBitmap(Utils.getBitmap(context, "menu.bmp"), 87, 87, null);
			
			
			Protocol.sendLcdBitmap(bitmap, MetaWatchService.WatchBuffers.NOTIFICATION);		
			Protocol.updateLcdDisplay(MetaWatchService.WatchBuffers.NOTIFICATION);
		} else {
			Bitmap icon = Utils.getBitmap(context, "phone.bmp");
			Notification
					.addOledNotification(context, Protocol.createOled1line(
							context, icon, "Call from"), Protocol
							.createOled1line(context, null, name), null, 0,
							new VibratePattern(true, 500, 500, 3), "Phonecall");
		}
		
		Thread ringer = new Thread(new CallVibrate());
		ringer.start();		
	}
	
	public static void endRinging(Context context) {
		isRinging = false;
		exitCall(context);
	}
		
	static void toCall() {		
		MetaWatchService.watchState = MetaWatchService.WatchStates.CALL;
		MetaWatchService.WatchModes.CALL = true;			
		
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Protocol.enableButton(0, 0, CALL_ANSWER, MetaWatchService.WatchBuffers.NOTIFICATION); // Right top
			Protocol.enableButton(1, 0, CALL_DISMISS, MetaWatchService.WatchBuffers.NOTIFICATION); // Right middle
			Protocol.enableButton(2, 0, CALL_MENU, MetaWatchService.WatchBuffers.NOTIFICATION); // Right bottom
		}

	}
	
	static void exitCall(Context context) {
				
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Protocol.disableButton(0, 0, MetaWatchService.WatchBuffers.NOTIFICATION); // Right top
			Protocol.disableButton(1, 0, MetaWatchService.WatchBuffers.NOTIFICATION); // Right middle
			Protocol.disableButton(2, 0, MetaWatchService.WatchBuffers.NOTIFICATION); // Right bottom
		}
		
		MetaWatchService.WatchModes.CALL = false;
				
		if (MetaWatchService.WatchModes.NOTIFICATION == true)
			Notification.replay(context);
		else if (MetaWatchService.WatchModes.APPLICATION == true)
			Application.toApp(context);
		else if (MetaWatchService.WatchModes.IDLE == true)
			Idle.toIdle(context);
	}
	
}


