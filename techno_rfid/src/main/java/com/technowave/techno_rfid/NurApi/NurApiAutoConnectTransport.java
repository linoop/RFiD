/* 
  Copyright 2016- Nordic ID 
  NORDIC ID DEMO SOFTWARE DISCLAIMER

  You are about to use Nordic ID Demo Software ("Software"). 
  It is explicitly stated that Nordic ID does not give any kind of warranties, 
  expressed or implied, for this Software. Software is provided "as is" and with 
  all faults. Under no circumstances is Nordic ID liable for any direct, special, 
  incidental or indirect damages or for any economic consequential damages to you 
  or to any third party.

  The use of this software indicates your complete and unconditional understanding 
  of the terms of this disclaimer. 
  
  IF YOU DO NOT AGREE OF THE TERMS OF THIS DISCLAIMER, DO NOT USE THE SOFTWARE.  
*/

package com.technowave.techno_rfid.NurApi;

public interface NurApiAutoConnectTransport {

	/** Should return the type of the implementing class (e.g. "BLE", "USB"). */
	public String getType();
	
	/** Can be used to provide some more information about the implementing class. */
	public String getDetails();
	
	/** Sets the address to ehich the implementing class should connect to. */
	public void setAddress(String addr);
	
	/** Returns the address where the implementing class is connected or connecting to. */
	public String getAddress();

	/** The method if the autoconnect implementation needs to handle "onPause". */
	public void onPause();

	/** The method if the autoconnect implementation needs to handle "onResume". */
	public void onResume();

	/** The method if the autoconnect implementation needs to handle "onStop". */
	public void onStop();

	/** The method if the autoconnect implementation needs to handle "onDestroy". */
	public void onDestroy();

	/** Disposing method if needed. */
	public void dispose();
}
