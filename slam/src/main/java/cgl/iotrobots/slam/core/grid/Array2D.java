package cgl.iotrobots.slam.core.grid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Array2D {
    private Logger LOG = LoggerFactory.getLogger(Array2D.class);

    public int m_xsize, m_ysize;
    public Cell m_cells[][];

    public Array2D(int xsize, int ysize){
        m_xsize=xsize;
        m_ysize=ysize;
        if (m_xsize>0 && m_ysize>0){
            m_cells = new Cell[m_xsize][m_ysize];
        } else{
            m_xsize=m_ysize=0;
            m_cells= null;
        }
        LOG.debug("m_xsize= " + m_xsize + " m_ysize: " + m_ysize);
    }

    public void assign(Array2D g) {
        if (m_xsize != g.m_xsize || m_ysize != g.m_ysize) {
            m_xsize = g.m_xsize;
            m_ysize = g.m_ysize;
            if (m_xsize > 0 && m_ysize > 0) {
                m_cells = new Cell[m_xsize][m_ysize];
            } else {
                m_xsize = m_ysize = 0;
                m_cells = null;
            }
        }
        for (int x = 0; x < m_xsize; x++) {
            System.arraycopy(g.m_cells[x], 0, m_cells[x], 0, m_ysize);
        }

        LOG.debug("m_xsize= " + m_xsize + " m_ysize: " + m_ysize);
    }

    public Array2D(Array2D g){
        assign(g);
    }

    public void clear(){
        LOG.debug("m_xsize= " + m_xsize + " m_ysize: " + m_ysize);
        m_cells=null;
        m_xsize=0;
        m_ysize=0;
    }

    public void resize(int xmin, int ymin, int xmax, int ymax){
        int xsize=xmax-xmin;
        int ysize=ymax-ymin;
        Cell [][] newcells = new Cell[xsize][ysize];
        int dx= xmin < 0 ? 0 : xmin;
        int dy= ymin < 0 ? 0 : ymin;
        int Dx = xmax< this.m_xsize? xmax: this.m_xsize;
        int Dy= ymax < this.m_ysize? ymax: this.m_ysize;
        for (int x=dx; x<Dx; x++){
            System.arraycopy(this.m_cells[x], dy, newcells[x - xmin], dy - ymin, Dy - dy);
        }
        this.m_cells=newcells;
        this.m_xsize=xsize;
        this.m_ysize=ysize;
    }

    public boolean isInside(int x, int y) {
        return x >=0 && y >=0 && x< m_xsize && y<m_ysize;
    }

    public Cell cell(int x, int y) {
        assert(isInside(x,y));
        return m_cells[x][y];
    }
}