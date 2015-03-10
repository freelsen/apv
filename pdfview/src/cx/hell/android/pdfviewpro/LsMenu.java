// created: 140302;
// purpose: menu stuff ls add;
// auther : freelsen@gmail.com;

package cx.hell.android.pdfviewpro;

import android.view.Menu;
import android.view.MenuItem;

public class LsMenu {
	
	private MenuItem mlsbarmenu = null;
	LsMenu()
	{
		
	}
	public void Add2Menu( Menu menu )
	{
		mlsbarmenu = menu.add(R.string.lsbar);
	}
	public boolean isLsbarMenu( MenuItem item)
	{
		return (mlsbarmenu == item);
	}
}
