package grabasilak.iti.www.myapplication;

import java.util.ArrayList;

class MeshPrimitive {

    ArrayList<Integer> vertices;
    ArrayList<Integer> normals;
    ArrayList<Integer> uvs;

    MeshPrimitive()
    {
        vertices = new ArrayList<>();
        normals  = new ArrayList<>();
        uvs      = new ArrayList<>();
    }
}
