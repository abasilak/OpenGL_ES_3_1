package grabasilak.iti.www.myapplication;

import java.util.ArrayList;

class Face3D {

    ArrayList<Integer> vertices;
    ArrayList<Integer> normals;
    ArrayList<Integer> uvs;

    Face3D()
    {
        vertices = new ArrayList<>();
        normals  = new ArrayList<>();
        uvs      = new ArrayList<>();
    }
}
