package grabasilak.iti.www.myapplication;

public class TextObject {
	
	public String 	m_text;
	public float  	m_x;
	public float  	m_y;
	public float[]  m_color;

	public TextObject()
	{
		m_text = "default";
		m_x = m_y = 0f;
		m_color = new float[] {0.5f, 0.5f, 0.5f, 1.0f};
	}
	
	public TextObject(String text, float x, float y)
	{
		m_text  = text;
		m_x 	= x;
		m_y 	= y;
		m_color = new float[] {0.5f, 0.5f, 0.5f, 1.0f};
	}
}
