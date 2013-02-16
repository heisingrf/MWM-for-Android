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
 * Application.java                                                          *
 * Application                                                               *
 * Application watch mode                                                    *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import org.metawatch.manager.MetaWatchService.WatchModes;
import org.metawatch.manager.apps.ApplicationBase;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

public class Application {
    public final static byte EXIT_APP = 100;
    private static ApplicationBase currentApp = null;
    public static void startAppMode(Context context, ApplicationBase internalApp) {
	if (currentApp != null) {
	    stopAppMode(context);
	    Idle.getInstance().toIdle(context);
	}
	MetaWatchService.watchMode.push(WatchModes.APPLICATION);
	currentApp = internalApp;
    }

    public static void stopAppMode(Context context) {
	MetaWatchService.watchMode.pop();
	if (currentApp != null) {
	    currentApp.deactivate(context, MetaWatchService.watchType);
	    currentApp.setInactive();
	}
	currentApp = null;
    }

    public static void updateAppMode(Context context) {
	Bitmap bitmap;
	if (currentApp != null) {
	    bitmap = currentApp.update(context, false, MetaWatchService.watchType);
	} else {
	    bitmap = Protocol.getInstance(context).createTextBitmap(context, "Starting application mode ...");
	}

	updateAppMode(context, bitmap);
    }

    public static void updateAppMode(Context context, Bitmap bitmap) {
	WatchModes currentMode = MetaWatchService.watchMode.peek();
	if (currentMode == WatchModes.APPLICATION) {
	    Protocol.getInstance(context).sendLcdBitmap(bitmap, MetaWatchService.WatchBuffers.APPLICATION);
	    Protocol.getInstance(context).configureIdleBufferSize(false);
	    Protocol.getInstance(context).updateLcdDisplay(MetaWatchService.WatchBuffers.APPLICATION);
	}
    }

    public static boolean toApp(final Context context) {
	Idle.getInstance().deactivateButtons(context);
	int watchType = MetaWatchService.watchType;
	if (currentApp != null && MetaWatchService.watchMode.peek() == MetaWatchService.WatchModes.APPLICATION) {
	    updateAppMode(context);
	    currentApp.activate(context, watchType);
	    if (watchType == MetaWatchService.WatchType.DIGITAL) {
		Protocol.getInstance(context).enableButton(0, 1, EXIT_APP, MetaWatchService.WatchBuffers.APPLICATION); // right
	    } else if (watchType == MetaWatchService.WatchType.ANALOG) {
		Protocol.getInstance(context).enableButton(1, 1, EXIT_APP, MetaWatchService.WatchBuffers.APPLICATION); // right
	    }
	    return true;
	}	
	return false;
    }

    public static void buttonPressed(Context context, int button) {
	if (button == EXIT_APP) {
	    stopAppMode(context);
	    Idle.getInstance().toIdle(context);
	} else if (currentApp != null) {
	    if (currentApp.buttonPressed(context, button) != ApplicationBase.BUTTON_USED_DONT_UPDATE) {
		updateAppMode(context);
	    }
	} else {
	    // Broadcast button to external app
	    Intent intent = new Intent("org.metawatch.manager.BUTTON_PRESS");
	    intent.putExtra("button", button);
	    context.sendBroadcast(intent);
	}
    }
}
