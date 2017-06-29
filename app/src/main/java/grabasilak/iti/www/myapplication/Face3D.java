package grabasilak.iti.www.myapplication;

import java.util.ArrayList;

class Face3D {

    ArrayList<Integer> vertices;
    ArrayList<Integer> textures;
    ArrayList<Integer> normals;

    Face3D()
    {
        vertices = new ArrayList<>();
        textures = new ArrayList<>();
        normals  = new ArrayList<>();
    }
}
