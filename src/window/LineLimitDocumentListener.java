package window;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

public class LineLimitDocumentListener implements DocumentListener{

	int maxLines;
	
	public LineLimitDocumentListener(int maxLines){
		this.maxLines = maxLines;
	}
	
	@Override
	public void insertUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub
		SwingUtilities.invokeLater( new Runnable()
		{
			public void run()
			{
				limitLines(e);
			}
		});
	}
	
	private void limitLines(DocumentEvent e)
	{
		//  The root Element of the Document will tell us the total number
		//  of line in the Document.

		Document document = e.getDocument();
		Element root = document.getDefaultRootElement();

		while (root.getElementCount() > maxLines)
		{
			Element line = root.getElement(0);
			int end = line.getEndOffset();

			try
			{
				document.remove(0, end);
			}
			catch(BadLocationException ble)
			{
				System.out.println(ble);
			}
		}
	}
	

	@Override
	public void removeUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub
		
	}

}
