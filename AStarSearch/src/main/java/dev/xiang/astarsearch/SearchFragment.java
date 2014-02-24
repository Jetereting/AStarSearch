package dev.xiang.astarsearch;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by xiang on 24/02/2014.
 */
public class SearchFragment extends Fragment {

    private static final int SIZE = 10;
    private static final String TAG = SearchFragment.class.getSimpleName();
    private GridAdapter mAdapter;
    private int mCellSize;

    public SearchFragment() {
    }

    private class Cell {
        public static final int EMPTY = 0xffffffff;
        public static final int WALL = 0xff000000;
        public static final int START = 0xffff0000;
        public static final int TARGET = 0xff00ff00;
        public static final int PATH = 0xffffff00;
        public int type;
        public int f, g, h;
        private int x, y;
        Cell from;

        public Cell(int x, int y) {
            type = EMPTY;
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    private GridView mGridView;
    private Cell mData[][] = new Cell[SIZE][SIZE];

    private RadioGroup mRadioGroup;
    private int mState = Cell.START;
    private Button mFindButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mGridView = (GridView) rootView.findViewById(R.id.grid);
        mRadioGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup);
        mFindButton = (Button) rootView.findViewById(R.id.run);
        mAdapter = new GridAdapter();
        mGridView.setAdapter(mAdapter);
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                mData[i][j] = new Cell(i, j);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId) {
                    case R.id.start:
                        mState = Cell.START;
                        break;
                    case R.id.end:
                        mState = Cell.TARGET;
                        break;
                    case R.id.wall:
                        mState = Cell.WALL;
                        break;
                    case R.id.eraser:
                        mState = Cell.EMPTY;
                    default:
                        break;
                }
            }
        });

        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int size = getResources().getDimensionPixelSize(R.dimen.cell_size);
                        int spacing = getResources().getDimensionPixelSize(R.dimen.cell_spacing);
                        int width = mGridView.getWidth();
                        mCellSize = (width - (SIZE - 1) * spacing) / SIZE;
                        mGridView.setColumnWidth(mCellSize);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            mGridView.getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(this);
                        } else {
                            mGridView.getViewTreeObserver()
                                    .removeGlobalOnLayoutListener(this);
                        }
                    }
                });

        mFindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateMap()) {
                    findPath();
                }
            }
        });
        return rootView;
    }


    private Cell mStartCell, mTargetCell;

    private boolean findPath() {
//        Toast.makeText(getActivity(), "Finding path.", Toast.LENGTH_LONG).show();
        HashSet<Cell> closeSet = new HashSet<Cell>();
        HashSet<Cell> openSet = new HashSet<Cell>();
        openSet.add(mStartCell);
        mStartCell.g = 0;
        mStartCell.f = mStartCell.g + estimate_h(mStartCell, mTargetCell);

        while (!openSet.isEmpty()) {
            Cell current = null;
            for (Cell c : openSet) {
                if (current == null)
                    current = c;
                else {
                    if (current.f > c.f)
                        current = c;
                }
            }

            if (current == mTargetCell) {
                Toast.makeText(getActivity(), "A path was found", Toast.LENGTH_LONG).show();
                while (current != mStartCell) {
                    Log.d(TAG, "PATH: " + current.x + "," + current.y);
                    current = current.from;
                    current.type = Cell.PATH;
                }
                Log.d(TAG, "PATH: " + current.x + "," + current.y);
                mStartCell.type = Cell.START;
                mGridView.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
                return true;
            }
            openSet.remove(current);
            closeSet.add(current);
            HashSet<Cell> neighbours = getNeighbours(current);
            for (Cell neighbour : neighbours) {
                if (closeSet.contains(neighbour))
                    continue;
                int tentativeG = current.g + 1;
                if (!openSet.contains(neighbour) || tentativeG < neighbour.g) {
                    neighbour.from = current;
                    neighbour.g = tentativeG;
                    neighbour.f = neighbour.g + estimate_h(neighbour, mTargetCell);
                    if (!openSet.contains(neighbour))
                        openSet.add(neighbour);
                }
            }
        }
        Toast.makeText(getActivity(), "Path not found", Toast.LENGTH_LONG).show();
        return false;
    }

    public void dump(Collection<Cell> collection) {
        for (Cell c : collection) {
            Log.d(TAG, "DUMP: " + c.x + "," + c.y);
        }
    }

    private HashSet<Cell> getNeighbours(Cell cell) {
        HashSet<Cell> neighbours = new HashSet<Cell>();
        int x = cell.x;
        int y = cell.y;
        if (x > 0)
            neighbours.add(mData[cell.x - 1][cell.y]);
        if (y > 0)
            neighbours.add(mData[cell.x][cell.y - 1]);
        if (x < SIZE - 1)
            neighbours.add(mData[cell.x + 1][cell.y]);
        if (y < SIZE - 1)
            neighbours.add(mData[cell.x][cell.y + 1]);
        Iterator<Cell> it = neighbours.iterator();
        HashSet<Cell> toDelete = new HashSet<Cell>();
        while (it.hasNext()) {
            Cell c = it.next();
            if (c.type == Cell.WALL)
                toDelete.add(c);
        }
        neighbours.removeAll(toDelete);
        return neighbours;
    }

    private int estimate_h(Cell start, Cell target) {
        int dist_x = Math.abs(start.x - target.x);
        int dist_y = Math.abs(start.y - target.y);
        start.h = dist_x + dist_y;
        return start.h;
    }

    private boolean validateMap() {

        if (mStartCell == null) {
            Toast.makeText(getActivity(), "Start point not found.", Toast.LENGTH_LONG).show();
            return false;
        } else if (mTargetCell == null) {
            Toast.makeText(getActivity(), "End point not found.", Toast.LENGTH_LONG).show();
            return false;
        } else
            return true;

    }

    private class GridAdapter extends BaseAdapter {

        private final LayoutInflater mInflater;

        private GridAdapter() {
            mInflater = LayoutInflater.from(getActivity());
        }

        @Override
        public int getCount() {
            return SIZE * SIZE;
        }

        @Override
        public Object getItem(int i) {
            return mData[i % 10][i / 10];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder holder = null;
            if (view == null) {
                holder = new ViewHolder();
                view = mInflater.inflate(R.layout.item_grid, viewGroup, false);
                GridView.LayoutParams params = new GridView.LayoutParams(mCellSize, mCellSize);
                view.setLayoutParams(params);
                holder.tl = (TextView) view.findViewById(R.id.tl);
                holder.tr = (TextView) view.findViewById(R.id.tr);
                holder.bl = (TextView) view.findViewById(R.id.bl);
                holder.br = (TextView) view.findViewById(R.id.br);
                holder.center = (TextView) view.findViewById(R.id.centre);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            final Cell cell = (Cell) getItem(i);
            final int index = i;

            holder.center.setText("" + i / 10 + "," + i % 10);
            view.setBackgroundColor(cell.type);

            if (cell.f != 0)
                holder.tl.setText("" + cell.f);
            if (cell.g != 0)
                holder.tr.setText("" + cell.g);
            if (cell.h != 0)
                holder.br.setText("" + cell.h);
            if (cell.from != null) {
                if (cell.from.x < cell.x)
                    holder.bl.setText("←");
                else if (cell.from.y < cell.y)
                    holder.bl.setText("↑");
                else if (cell.from.x > cell.x)
                    holder.bl.setText("→");
                else if (cell.from.y > cell.y)
                    holder.bl.setText("↓");
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mState == Cell.START || mState == Cell.TARGET) { // allow only one start and one end
                        for (int i = 0; i < getCount(); i++) {
                            Cell item = (Cell) getItem(i);
                            if (item.type == mState)
                                item.type = Cell.EMPTY;
                        }
                        if (mState == Cell.START) {
                            mStartCell = cell;
                        } else if (mState == Cell.TARGET) {
                            mTargetCell = cell;
                        }
                    }
                    cell.type = mState;
                    Log.d("EEE", "onClick() index=" + index + " mState=" + mState);
                    notifyDataSetChanged();
                }
            }

            );
            return view;
        }

        class ViewHolder {
            TextView tl, tr, bl, br, center;
        }
    }
}
