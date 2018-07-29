package com.example.android.employeesmanagementapp.activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.android.employeesmanagementapp.NotificationService;
import com.example.android.employeesmanagementapp.R;
import com.example.android.employeesmanagementapp.RecyclerViewItemClickListener;
import com.example.android.employeesmanagementapp.adapters.DepartmentsArrayAdapter;
import com.example.android.employeesmanagementapp.adapters.EmployeesAdapter;
import com.example.android.employeesmanagementapp.adapters.HorizontalEmployeeAdapter;
import com.example.android.employeesmanagementapp.data.AppDatabase;
import com.example.android.employeesmanagementapp.data.AppExecutor;
import com.example.android.employeesmanagementapp.data.entries.DepartmentEntry;
import com.example.android.employeesmanagementapp.data.entries.EmployeeEntry;
import com.example.android.employeesmanagementapp.data.entries.TaskEntry;
import com.example.android.employeesmanagementapp.data.factories.DepIdFact;
import com.example.android.employeesmanagementapp.data.factories.TaskIdFact;
import com.example.android.employeesmanagementapp.data.viewmodels.AddNewDepViewModel;
import com.example.android.employeesmanagementapp.data.viewmodels.AddNewTaskViewModel;
import com.example.android.employeesmanagementapp.fragments.DatePickerFragment;
import com.example.android.employeesmanagementapp.utils.AppUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.Inflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AddTaskActivity extends AppCompatActivity implements RecyclerViewItemClickListener, EmployeesAdapter.CheckBoxClickListener {

    public static final String TASK_ID_KEY = "task_id";
    private static final String TAG = AddTaskActivity.class.getSimpleName();
    private static final int DEFAULT_TASK_ID = -1;
    private int mTaskId;

    private BottomSheetBehavior mSheetBehavior;

    private EmployeesAdapter mEmplyeesAdapter;

    private EditText mTaskTitle;
    private EditText mTaskDescription;
    private TextView mTaskStartDate;
    private TextView mTaskDueDate;
    private Spinner mTaskDepartment;
    private Toolbar mToolbar;
    private ImageView addTaskEmployees;

    private int mSelectedDepartmentId;

    private List<Integer> mTaskEmployeesIds;

    private AppDatabase mDb;

    private DepartmentsArrayAdapter mDepartmentsArrayAdapter;
    private RecyclerView mRecyclerView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        mDb = AppDatabase.getInstance(this);

        //check if activity was opened from a click on rv item or from the fab
        Intent intent = getIntent();
        if (intent != null) {
            mTaskId = intent.getIntExtra(TASK_ID_KEY, DEFAULT_TASK_ID);
        }


        //set toolbar as actionbar
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        //set toolbar home icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);


        //get views
        mTaskTitle = findViewById(R.id.task_title);
        mTaskDescription = findViewById(R.id.task_description);
        mTaskStartDate = findViewById(R.id.task_start_date);
        mTaskDueDate = findViewById(R.id.task_due_date);
        mTaskDepartment = findViewById(R.id.task_department);
        addTaskEmployees = findViewById(R.id.add_more_employees);
        addTaskEmployees.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChooseDepDialog();
            }
        });


        mDepartmentsArrayAdapter = new DepartmentsArrayAdapter(this, this);
        mTaskDepartment.setAdapter(mDepartmentsArrayAdapter);


        setUpToolBar();

        if (mTaskId == DEFAULT_TASK_ID) {
            clearViews();
        } else {
            final LiveData<TaskEntry> task = ViewModelProviders.of(this, new TaskIdFact(mDb, mTaskId)).get(AddNewTaskViewModel.class).getTask();
            task.observe(this, new Observer<TaskEntry>() {
                @Override
                public void onChanged(@Nullable TaskEntry taskEntry) {
                    task.removeObserver(this);
                    populateUI(taskEntry);
                }
            });
        }


        //allow scrolling of edit text content when it is inside a scroll view
        mTaskDescription.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return false;
            }
        });


        mTaskDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSelectedDepartmentId = (int) view.getTag();
                //populateBottomSheet(mSelectedDepartmentId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        setUpEmployeesRV();
    }

    private void showChooseDepDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose employees :");

        RecyclerView chooseEmployeesRV = new RecyclerView(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        chooseEmployeesRV.setLayoutManager(linearLayoutManager);
        chooseEmployeesRV.setHasFixedSize(true);

        ChooseEmployeesAdapter chooseEmployeesAdapter = new ChooseEmployeesAdapter();
        chooseEmployeesAdapter.setData(AppUtils.getEmployeesFakeData());
        chooseEmployeesRV.setAdapter(chooseEmployeesAdapter);

        builder.setView(chooseEmployeesRV);

        builder.setPositiveButton(getString(R.string.choose_department_dialog_positive_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setNegativeButton(getString(R.string.choose_department_dialog_cancel_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void setUpEmployeesRV() {
        mRecyclerView = findViewById(R.id.department_employees_rv);

        mRecyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        mRecyclerView.setLayoutManager(layoutManager);

        HorizontalEmployeeAdapter mAdapter = new HorizontalEmployeeAdapter(this, true);
        mAdapter.setData(AppUtils.getEmployeesFakeData());
        mRecyclerView.setAdapter(mAdapter);
    }

    private void clearViews() {
        mTaskTitle.setText("");
        mTaskDescription.setText("");
        mTaskStartDate.setText("");
        mTaskDueDate.setText("");
        mSelectedDepartmentId = 0;
        mTaskDepartment.setSelection(mSelectedDepartmentId);
    }

    private void populateUI(TaskEntry taskEntry) {
        if (taskEntry == null)
            return;

        mTaskTitle.setText(taskEntry.getTaskTitle());
        mTaskDescription.setText(taskEntry.getTaskDescription());
        mTaskStartDate.setText(taskEntry.getTaskDueDate().toString());
        mTaskDueDate.setText(taskEntry.getTaskDueDate().toString());

        final LiveData<DepartmentEntry> employeeDep = ViewModelProviders.of(this, new DepIdFact(mDb, taskEntry.getDepartmentID())).get(AddNewDepViewModel.class).getDepartment();
        employeeDep.observe(this, new Observer<DepartmentEntry>() {
            @Override
            public void onChanged(DepartmentEntry departmentEntry) {
                employeeDep.removeObserver(this);
                mTaskDepartment.setSelection(mDepartmentsArrayAdapter.getPositionForItemId(departmentEntry));
            }
        });

    }


    private void setUpToolBar() {
        if (mTaskId == DEFAULT_TASK_ID) {
            getSupportActionBar().setTitle(getString(R.string.add_new_task));
        } else {
            getSupportActionBar().setTitle(getString(R.string.edit_task));
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
//        Intent intent = new Intent(this, NotificationService.class);
//        // send the due date and the id of the task within the intent
//        //intent.putExtra("task due date", taskDueDate.getTime() - taskStartDAte.getTime())'
//        //intent.putExtra("task id",mTaskId);
//
//        //just for experiment
//        Bundle bundle = new Bundle();
//        bundle.putInt("task id",mTaskId);
//        bundle.putLong("task due date",5);
//        intent.putExtras(bundle);
//        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveTask();
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void saveTask() {
        if (valideData()) {
            int departmentId = (int) mTaskDepartment.getSelectedView().getTag();
            String taskTitle = mTaskTitle.getText().toString();
            String taskDescription = mTaskDescription.getText().toString();
            //todo:change string date to java object date
            Date taskStartDate = new Date();
            Date taskDueDate = new Date();


            final TaskEntry newTask = new TaskEntry(departmentId, taskTitle, taskDescription, taskStartDate, taskDueDate);

            AppExecutor.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    if (mTaskId == DEFAULT_TASK_ID) {
                        mDb.tasksDao().addTask(newTask);
                        System.out.println("new task");
                    } else {
                        newTask.setTaskId(mTaskId);
                        mDb.tasksDao().updateTask(newTask);
                        System.out.println("update task");
                    }
                }
            });
        }
        finish();
    }


    private boolean valideData() {
        return true;
    }


    public void pickDate(View view) {
        //create a bundle containing id of clicked text view (startDateTextView or dueDateTextView)
        Bundle bundle = new Bundle();
        bundle.putInt("date_view_id", view.getId());

        //instantiate a DatePickerFragment to show date picker dialog
        DialogFragment datePickerFragment = new DatePickerFragment();
        datePickerFragment.setArguments(bundle);

        //show th dialog
        datePickerFragment.show(getSupportFragmentManager(), "datePicker");
    }

    @Override
    public void onCheckBoxClicked(int employeeID) {

    }

    @Override
    public void onItemClick(int clickedItemRowID, int clickedItemPosition) {

        Intent intent = new Intent(this, AddEmployeeActivity.class);
        intent.putExtra(AddEmployeeActivity.EMPLOYEE_VIEW_ONLY, true);
        intent.putExtra(AddEmployeeActivity.EMPLOYEE_ID_KEY, clickedItemRowID);
        startActivity(intent);
    }
}

//Adapter for ALertDialog RecyclerView
class ChooseEmployeesAdapter extends RecyclerView.Adapter<ChooseEmployeesAdapter.chooseEmployeeViewHolder> {
    private List<EmployeeEntry> mData;
    private final SparseBooleanArray array=new SparseBooleanArray();


    @NonNull
    @Override
    public chooseEmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.choose_employee_item, parent, false);
        chooseEmployeeViewHolder chooseEmployeeViewHolder = new chooseEmployeeViewHolder(rootView);
        return chooseEmployeeViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull chooseEmployeeViewHolder holder, int position) {
        if(array.get(position)){
            holder.employeeCheckBox.setChecked(true);
        }else{
            holder.employeeCheckBox.setChecked(false);
        }
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setData(List<EmployeeEntry> data) {
        mData = data;
    }

    public class chooseEmployeeViewHolder extends RecyclerView.ViewHolder {

        TextView employeeName;
        CheckBox employeeCheckBox;

        public chooseEmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            employeeName = itemView.findViewById(R.id.choose_employee_name);
            employeeCheckBox = itemView.findViewById(R.id.choose_employee_check_box);
            employeeCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    array.put(getAdapterPosition(),true);
                }
            });
        }

        public void bind(int position) {
            employeeName.setText(mData.get(position).getEmployeeName());
        }
    }

}