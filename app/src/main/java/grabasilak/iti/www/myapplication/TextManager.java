package grabasilak.iti.www.myapplication;

import android.opengl.GLES31;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Iterator;
import java.util.Vector;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_SHORT;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES30.glBindVertexArray;
import static android.opengl.GLES30.glDrawRangeElements;
import static android.opengl.GLES30.glGenVertexArrays;
import static android.opengl.GLES31.GL_FLOAT;
import static android.opengl.GLES31.glEnableVertexAttribArray;
import static android.opengl.GLES31.glGetUniformLocation;
import static android.opengl.GLES31.glUniform1i;
import static android.opengl.GLES31.glUniformMatrix4fv;
import static android.opengl.GLES31.glUseProgram;
import static android.opengl.GLES31.glVertexAttribPointer;

public class TextManager {

	boolean	m_enabled = true;

	private static final float RI_TEXT_UV_BOX_WIDTH = 0.125f;
	private static final float RI_TEXT_WIDTH = 32.0f;
	private static final float RI_TEXT_SPACESIZE = 20f;

	private FloatBuffer vertexBuffer;
	private FloatBuffer textureBuffer;
	private FloatBuffer colorBuffer;
	private ShortBuffer drawListBuffer;

	private float[] vecs;
	private float[] uvs;
	private short[] indices;
	private float[] colors;

	private int index_vecs;
	private int index_indices;
	private int index_uvs;
	private int index_colors;

    // Vertex Array Object
    private final int []        m_vao             = new int[1];
    // Vertex Buffer Objects
    private final int []        m_vertices_vbo    = new int[1];
    private final int []        m_colors_vbo      = new int[1];
    private final int []        m_uvs_vbo         = new int[1];
    private final int []        m_indices_vbo     = new int[1];

	private int texturenr;

	private float uniformscale;

	public static int[] l_size = {36,29,30,34,25,25,34,33,
								   11,20,31,24,48,35,39,29,
								   42,31,27,31,34,35,46,35,
								   31,27,30,26,28,26,31,28,
								   28,28,29,29,14,24,30,18,
								   26,14,14,14,25,28,31,0,
								   0,38,39,12,36,34,0,0,
								   0,38,0,0,0,0,0,0};

	public Vector<TextObject> txtcollection;

	public TextManager()
	{
		// Create our container
		txtcollection = new Vector<>();

		// Create the arrays
		vecs 			= new float[3 * 10];
		colors 			= new float[4 * 10];
		uvs 			= new float[2 * 10];
		indices 		= new short[10];

		// init as 0 as default
		texturenr = 0;
	}

    public void clear()
    {
        txtcollection.clear();
    }

	public void addText(TextObject obj)
	{
		txtcollection.add(obj);
	}

	public void setTextureID(int val)
	{
		texturenr = val;
	}


	public void AddCharRenderInformation(float[] vec, float[] cs, float[] uv, short[] indi)
	{
		// We need a base value because the object has indices related to
		// that object and not to this collection so basicly we need to
		// translate the indices to align with the vertexlocation in ou
		// vecs array of vectors.
		short base = (short) (index_vecs / 3);

		// We should add the vec, translating the indices to our saved vector
		for(int i=0;i<vec.length;i++)
		{
			vecs[index_vecs] = vec[i];
			index_vecs++;
		}

		// We should add the colors, so we can use the same texture for multiple effects.
		for(int i=0;i<cs.length;i++)
		{
			colors[index_colors] = cs[i];
			index_colors++;
		}

		// We should add the uvs
		for(int i=0;i<uv.length;i++)
		{
			uvs[index_uvs] = uv[i];
			index_uvs++;
		}

		// We handle the indices
		for(int j=0;j<indi.length;j++)
		{
			indices[index_indices] = (short) (base + indi[j]);
			index_indices++;
		}
	}

	public void PrepareDrawInfo()
	{
		// Reset the indices.
		index_vecs = 0;
		index_indices = 0;
		index_uvs = 0;
		index_colors = 0;

		// Get the total amount of characters
		int charcount = 0;
		for (TextObject txt : txtcollection) {
			if(txt!=null)
			{
				if(!(txt.m_text==null))
				{
					charcount += txt.m_text.length();
				}
			}
		}

		// Create the arrays we need with the correct size.
		vecs = null;
		colors = null;
		uvs = null;
		indices = null;

		vecs = new float[charcount * 12];
		colors = new float[charcount * 16];
		uvs = new float[charcount * 8];
		indices = new short[charcount * 6];

	}

	public void PrepareDraw()
	{
		// Setup all the arrays
		PrepareDrawInfo();

		// Using the iterator protects for problems with concurrency
		for( Iterator< TextObject > it = txtcollection.iterator(); it.hasNext() ; )
	    {
	    	TextObject txt = it.next();
	    	if(txt!=null)
			{
		    	if(!(txt.m_text==null))
				{
					convertTextToTriangleInfo(txt);
				}
			}
	    }

        createBuffers();
        createVBOs();
        createVAO();
	}

    public void createBuffers()
    {
        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(vecs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vecs);
        vertexBuffer.position(0);

        // The colors buffer.
        ByteBuffer bb3 = ByteBuffer.allocateDirect(colors.length * 4);
        bb3.order(ByteOrder.nativeOrder());
        colorBuffer = bb3.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);

        // The texture buffer
        ByteBuffer bb2 = ByteBuffer.allocateDirect(uvs.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureBuffer = bb2.asFloatBuffer();
        textureBuffer.put(uvs);
        textureBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);

    }

    private void createVBOs()
    {
        // Generate VBO Ids and load the VBOs with data
        glGenBuffers ( 1, m_vertices_vbo, 0 );
        {
            glBindBuffer(GL_ARRAY_BUFFER, m_vertices_vbo[0]);
            vertexBuffer.position(0);
            glBufferData(GL_ARRAY_BUFFER, vecs.length * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        }
        glBindBuffer ( GL_ARRAY_BUFFER, 0 );

        glGenBuffers(1, m_colors_vbo, 0);
        {
            glBindBuffer(GL_ARRAY_BUFFER, m_colors_vbo[0]);
            colorBuffer.position(0);
            glBufferData(GL_ARRAY_BUFFER, colors.length * Float.BYTES, colorBuffer, GL_STATIC_DRAW);
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glGenBuffers(1, m_uvs_vbo, 0);
        {
            glBindBuffer(GL_ARRAY_BUFFER, m_uvs_vbo[0]);
            textureBuffer.position(0);
            glBufferData(GL_ARRAY_BUFFER, uvs.length * Float.BYTES, textureBuffer, GL_STATIC_DRAW);
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glGenBuffers ( 1, m_indices_vbo, 0 );
        {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_indices_vbo[0]);
            drawListBuffer.position(0);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices.length * Short.BYTES, drawListBuffer, GL_STATIC_DRAW);
        }
        glBindBuffer ( GL_ELEMENT_ARRAY_BUFFER, 0 );
    }

    private void createVAO()
    {
        // Generate VAO Id
        glGenVertexArrays ( 1, m_vao, 0 );
        // Bind the VAO and then setup the vertex attributes
        glBindVertexArray ( m_vao[0] );
        {
            glBindBuffer(GL_ARRAY_BUFFER, m_vertices_vbo[0]);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, Float.BYTES * 3, 0);

            glBindBuffer(GL_ARRAY_BUFFER, m_colors_vbo[0]);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 4, GL_FLOAT, false, Float.BYTES * 4, 0);

            glBindBuffer(GL_ARRAY_BUFFER, m_uvs_vbo[0]);
            glEnableVertexAttribArray(2);
            glVertexAttribPointer(2, 2, GL_FLOAT, false, Float.BYTES * 2, 0);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_indices_vbo[0]);
        }
        // Reset to the default VAO
        glBindVertexArray ( 0 );
    }

	public void Draw(int program, int mScreenWidth, int mScreenHeight)
	{
        // Set the correct shader for our grid object.
		glUseProgram(program);

		float[] mProjection = new float[16];
		float[] mView = new float[16];
		float[] mProjView = new float[16];

		Matrix.orthoM(mProjection, 0, 0f, mScreenWidth, 0.0f, mScreenHeight, 0, 50);

		// Set the camera position (View matrix)
		Matrix.setLookAtM(mView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

		// Calculate the projection and view transformation
		Matrix.multiplyMM(mProjView, 0, mProjection, 0, mView, 0);

    	// Apply the projection and view transformation
        glUniformMatrix4fv(glGetUniformLocation(program, "uniform_mvp"), 1, false, mProjView, 0);

        // Set the sampler texture unit to our selected id
	    glUniform1i (  glGetUniformLocation (program, "font_texture" ), 0);

		GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
		GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texturenr);

		// 3. DRAW
        glBindVertexArray ( m_vao[0] );
        {
            glDrawRangeElements(GL_TRIANGLES, 0, indices.length, indices.length, GL_UNSIGNED_SHORT, 0);
        }
        glBindVertexArray ( 0 );

        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0);

        glUseProgram(0);
	}

	private int convertCharToIndex(int c_val)
	{
		int indx = -1;

		// Retrieve the index
		if(c_val>64&&c_val<91) // A-Z
			indx = c_val - 65;
		else if(c_val>96&&c_val<123) // a-z
			indx = c_val - 97;
		else if(c_val>47&&c_val<58) // 0-9
			indx = c_val - 48 + 26;
		else if(c_val==43) // +
			indx = 38;
		else if(c_val==45) // -
			indx = 39;
		else if(c_val==33) // !
			indx = 36;
		else if(c_val==63) // ?
			indx = 37;
		else if(c_val==61) // =
			indx = 40;
		else if(c_val==58) // :
			indx = 41;
		else if(c_val==46) // .
			indx = 42;
		else if(c_val==44) // ,
			indx = 43;
		else if(c_val==42) // *
			indx = 44;
		else if(c_val==36) // $
			indx = 45;

		return indx;
	}

	private void convertTextToTriangleInfo(TextObject val)
	{
		// Get attributes from text object
		float x = val.m_x;
		float y = val.m_y;
		String text = val.m_text;

		// Create
		for(int j=0; j<text.length(); j++)
		{
			// get ascii value
			char c = text.charAt(j);
			int c_val = (int)c;

			int indx = convertCharToIndex(c_val);

			if(indx==-1) {
				// unknown character, we will add a space for it to be save.
				x += ((RI_TEXT_SPACESIZE) * uniformscale);
				continue;
			}

			// Calculate the uv parts
			int row = indx / 8;
			int col = indx % 8;

			float v = row * RI_TEXT_UV_BOX_WIDTH;
			float v2 = v + RI_TEXT_UV_BOX_WIDTH;
			float u = col * RI_TEXT_UV_BOX_WIDTH;
			float u2 = u + RI_TEXT_UV_BOX_WIDTH;

			// Creating the triangle information
			float[] vec = new float[12];
			float[] uv = new float[8];
			float[] colors = new float[16];

			vec[0] = x;
			vec[1] = y + (RI_TEXT_WIDTH * uniformscale);
			vec[2] = 0.99f;
			vec[3] = x;
			vec[4] = y;
			vec[5] = 0.99f;
			vec[6] = x + (RI_TEXT_WIDTH * uniformscale);
			vec[7] = y;
			vec[8] = 0.99f;
			vec[9] = x + (RI_TEXT_WIDTH * uniformscale);
			vec[10] = y + (RI_TEXT_WIDTH * uniformscale);
			vec[11] = 0.99f;

            colors = new float[]
                    {val.m_color[0], val.m_color[1], val.m_color[2], val.m_color[3],
                            val.m_color[0], val.m_color[1], val.m_color[2], val.m_color[3],
                            val.m_color[0], val.m_color[1], val.m_color[2], val.m_color[3],
                            val.m_color[0], val.m_color[1], val.m_color[2], val.m_color[3]
                    };

			// 0.001f = texture bleeding hack/fix
			uv[0] = u+0.001f;
			uv[1] = v+0.001f;
			uv[2] = u+0.001f;
			uv[3] = v2-0.001f;
			uv[4] = u2-0.001f;
			uv[5] = v2-0.001f;
			uv[6] = u2-0.001f;
			uv[7] = v+0.001f;

			short[] inds = {0, 1, 2, 0, 2, 3};

			// Add our triangle information to our collection for 1 render call.
			AddCharRenderInformation(vec, colors, uv, inds);

			// Calculate the new position
			x += ((l_size[indx]/2)  * uniformscale);
		}
	}

	public void setUniformscale(float uniformscale) {
		this.uniformscale = uniformscale;
	}
}
