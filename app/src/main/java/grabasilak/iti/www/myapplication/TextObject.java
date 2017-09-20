package grabasilak.iti.www.myapplication;

class TextObject {
	
	String 	m_text;
	float  	m_x;
	float  	m_y;
	float[]  m_color;

	TextObject()
	{
		m_text = "default";
		m_x = m_y = 0f;
		m_color = new float[] {0.5f, 0.5f, 0.5f, 1.0f};
	}
	
	TextObject(String text, float x, float y)
	{
		m_text  = text;
		m_x 	= x;
		m_y 	= y;
		m_color = new float[] {0.5f, 0.5f, 0.5f, 1.0f};
	}
}
