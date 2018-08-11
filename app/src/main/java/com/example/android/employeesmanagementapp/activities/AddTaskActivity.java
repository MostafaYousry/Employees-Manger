package com.example.android.employeesmanagementapp.activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.android.employeesmanagementapp.R;
import com.example.android.employeesmanagementapp.adapters.DepartmentsArrayAdapter;
import com.example.android.employeesmanagementapp.adapters.EmployeesAdapter;
import com.example.android.employeesmanagementapp.adapters.HorizontalEmployeeAdapter;
import com.example.android.employeesmanagementapp.data.AppDatabase;
import com.example.android.employeesmanagementapp.data.AppExecutor;
import com.example.android.employeesmanagementapp.data.entries.DepartmentEntry;
import com.example.android.employeesmanagementapp.data.entries.EmployeeEntry;
import com.example.android.employeesmanagementapp.data.entries.EmployeesTasksEntry;
import com.example.android.employeesmanagementapp.data.entries.TaskEntry;
import com.example.android.employeesmanagementapp.data.factories.TaskIdFact;
import com.example.android.employeesmanagementapp.data.viewmodels.AddNewTaskViewModel;
import com.example.android.employeesmanagementapp.utils.AppUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AddTaskActivity extends AppCompatActivity implements EmployeesAdapter.EmployeeItemClickListener {

    public static final String TASK_ID_KEY = "task_id";
    private static final String TAG = AddTaskActivity.class.getSimpleName();
    private static final int DEFAULT_TASK_ID = -1;
    boolean departmentsLoaded = false;
    private EditText mTaskTitle;
    private EditText mTaskDescription;
    private TextView mTaskStartDate;
    private TextView mTaskDueDate;
    private Spinner mTaskDepartment;
    private Toolbar mToolbar;
    private RecyclerView mTaskEmployeesRV;
    private int mTaskId;
    private HorizontalEmployeeAdapter mHorizontalEmployeeAdapter;
    private DepartmentsArrayAdapter mDepartmentsArrayAdapter;
    private AppDatabase mDb;
    private AddNewTaskViewModel mViewModel;
    private int clickedTaskDepId = -1;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        //bind views
        mTaskTitle = findViewById(R.id.task_title);
        mTaskDescription = findViewById(R.id.task_description);
        mTaskStartDate = findViewById(R.id.task_start_date);
        mTaskDueDate = findViewById(R.id.task_due_date);
        mTaskDepartment = findViewById(R.id.task_department);
        mToolbar = findViewById(R.id.toolbar);
        mTaskEmployeesRV = findViewById(R.id.task_employees_rv);

        mDb = AppDatabase.getInstance(this);

        //check if activity was opened from a click on rv item or from the fab
        Intent intent = getIntent();
        if (intent != null) {
            mTaskId = intent.getIntExtra(TASK_ID_KEY, DEFAULT_TASK_ID);
        }

        mViewModel = ViewModelProviders.of(this, new TaskIdFact(mDb, mTaskId)).get(AddNewTaskViewModel.class);


        //set toolbar as actionbar
        setSupportActionBar(mToolbar);

        //set toolbar home icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);


        mDepartmentsArrayAdapter = new DepartmentsArrayAdapter(this);
        LiveData<List<DepartmentEntry>> departments = mViewModel.getAllDepartments();


        departments.observe(this, new Observer<List<DepartmentEntry>>() {
            @Override
            public void onChanged(List<DepartmentEntry> departmentEntries) {
                mDepartmentsArrayAdapter.setData(departmentEntries);
                departmentsLoaded = true;
                if (clickedTaskDepId != -1) {
                    mTaskDepartment.setSelection(mDepartmentsArrayAdapter.getPositionForItemId(clickedTaskDepId));
                }
            }
        });


        mTaskDepartment.setAdapter(mDepartmentsArrayAdapter);


        setUpToolBar();

        if (mTaskId == DEFAULT_TASK_ID) {
            clearViews();
        } else {
            final LiveData<TaskEntry> task = mViewModel.getTask();
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


        setUpTaskEmployeesRV();

    }

    public void showChooseTaskEmployeesDialog(View view) {

        final int depId;
        List<EmployeeEntry> exceptThese;
        if (mTaskId == DEFAULT_TASK_ID) {
            depId = (int) mTaskDepartment.getSelectedView().getTag();
            if (mHorizontalEmployeeAdapter.getItemCount() == 0)
                exceptThese = null;
            else
                exceptThese = mHorizontalEmployeeAdapter.getAddedEmployees();
        } else {
            depId = mViewModel.getTask().getValue().getDepartmentID();
            exceptThese = mHorizontalEmployeeAdapter.getAllEmployees();
        }


        final LiveData<List<EmployeeEntry>> restOfEmployees = mViewModel.getRestOfEmployeesInDep(depId, exceptThese);
        restOfEmployees.observe(this, new Observer<List<EmployeeEntry>>() {
            @Override
            public void onChanged(final List<EmployeeEntry> employeeEntries) {
                restOfEmployees.removeObservers(AddTaskActivity.this);
                if (employeeEntries != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AddTaskActivity.this);
                    if (employeeEntries.isEmpty()) {
                        builder.setTitle("Empty department");
                        builder.setMessage("No employees in department please choose another one");
                        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        builder.show();

                    } else {
                        builder.setTitle("Employees available");
                        builder.setCancelable(false);
                        final List<EmployeeEntry> chosenOnes = new ArrayList<>();

                        final boolean[] isChecked = new boolean[employeeEntries.size()];

                        builder.setMultiChoiceItems(getEmployeeNames(employeeEntries), isChecked, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i, boolean checked) {
                                isChecked[i] = checked;

                                if (checked) {
                                    chosenOnes.add(employeeEntries.get(i));
                                } else {
                                    chosenOnes.remove(employeeEntries.get(i));
                                }
                            }
                        });

                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mHorizontalEmployeeAdapter.setAddedEmployees(chosenOnes);
                            }
                        });
                        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        builder.show();
                    }

                }

            }
        });

    }

    private String[] getEmployeeNames(List<EmployeeEntry> employeeEntries) {

        String[] arr = new String[employeeEntries.size()];
        for (int i = 0; i < employeeEntries.size(); i++) {
            arr[i] = employeeEntries.get(i).getEmployeeName();
        }

        return arr;
    }


    private void setUpTaskEmployeesRV() {

        mTaskEmployeesRV.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        mTaskEmployeesRV.setLayoutManager(layoutManager);

        mHorizontalEmployeeAdapter = new HorizontalEmployeeAdapter(this, true);
        mTaskEmployeesRV.setAdapter(mHorizontalEmployeeAdapter);

        if (mTaskId == DEFAULT_TASK_ID) {
            mHorizontalEmployeeAdapter.setData(new ArrayList<EmployeeEntry>());
        } else {
            final LiveData<List<EmployeeEntry>> taskEmployees = mViewModel.getTaskEmployees();
            taskEmployees.observe(this, new Observer<List<EmployeeEntry>>() {
                @Override
                public void onChanged(List<EmployeeEntry> employeeEntries) {
                    taskEmployees.removeObservers(AddTaskActivity.this);
                    mHorizontalEmployeeAdapter.setData(employeeEntries);
                }
            });
        }
    }

    private void clearViews() {
        mTaskTitle.setText("");
        mTaskDescription.setText("");
        mTaskStartDate.setText("");
        mTaskDueDate.setText("");
        mTaskDepartment.setSelection(0);

        mTaskDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mHorizontalEmployeeAdapter.clearAdapter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

    }

    private void populateUI(TaskEntry taskEntry) {
        if (taskEntry == null)
            return;

        clickedTaskDepId = taskEntry.getDepartmentID();
        mTaskTitle.setText(taskEntry.getTaskTitle());
        mTaskDescription.setText(taskEntry.getTaskDescription());
        mTaskStartDate.setText(AppUtils.getFriendlyDate(taskEntry.getTaskStartDate()));
        mTaskStartDate.setTag(taskEntry.getTaskStartDate());
        mTaskDueDate.setText(AppUtils.getFriendlyDate(taskEntry.getTaskDueDate()));
        mTaskDueDate.setTag(taskEntry.getTaskDueDate());

        if (departmentsLoaded)
            mTaskDepartment.setSelection(mDepartmentsArrayAdapter.getPositionForItemId(taskEntry.getDepartmentID()));

        mTaskDepartment.setEnabled(false);

//        mTaskDepartment.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(AddTaskActivity.this);
//                builder.setMessage("Can't change department of an already running task.If you want you can delete this task and create a new one.");
//                builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        dialogInterface.dismiss();
//                    }
//                });
//                builder.show();
//            }
//        });


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
//        Bundle bundle = new Bundle();
//        bundle.putInt("task id",mTaskId);
//        try {
//            bundle.putLong("task due date",FORMAT.parse(mTaskDueDate.toString()).getTime() - new Date().getTime());
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//        intent.putExtras(bundle);
//        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
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
        if (isValidData()) {
            int departmentId = (int) mTaskDepartment.getSelectedView().getTag();
            String taskTitle = mTaskTitle.getText().toString();
            String taskDescription = mTaskDescription.getText().toString();
            Date taskStartDate = (Date) mTaskStartDate.getTag();
            Date taskDueDate = (Date) mTaskDueDate.getTag();

            final TaskEntry newTask = new TaskEntry(departmentId, taskTitle, taskDescription, taskStartDate, taskDueDate);

            AppExecutor.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    if (mTaskId == DEFAULT_TASK_ID) {
                        mTaskId = (int) mDb.tasksDao().addTask(newTask);
                    } else {
                        newTask.setTaskId(mTaskId);
                        mDb.tasksDao().updateTask(newTask);
                    }
                }
            });

            final List<EmployeeEntry> removedEmployees = mHorizontalEmployeeAdapter.getRemovedEmployees();
            if (removedEmployees != null && !removedEmployees.isEmpty())
                AppExecutor.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < removedEmployees.size(); i++) {
                            EmployeesTasksEntry deletedEmployeeTask = new EmployeesTasksEntry(removedEmployees.get(i).getEmployeeID(), mTaskId);
                            mDb.employeesTasksDao().deleteEmployeeTask(deletedEmployeeTask);
                        }
                    }
                });


            final List<EmployeeEntry> addedEmployees = mHorizontalEmployeeAdapter.getAddedEmployees();
            if (addedEmployees != null && !addedEmployees.isEmpty())
                AppExecutor.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < addedEmployees.size(); i++) {
                            EmployeesTasksEntry newEmployeeTask = new EmployeesTasksEntry(addedEmployees.get(i).getEmployeeID(), mTaskId);
                            mDb.employeesTasksDao().addEmployeeTask(newEmployeeTask);
                        }
                    }
                });


        }
        finish();
    }


    private boolean isValidData() {
        if (true)
            return true;
        if (TextUtils.isEmpty(mTaskTitle.getText())) {
            showError("name");
            return false;
        }
        if (TextUtils.isEmpty(mTaskDescription.getText())) {
            showError("description");
            return false;
        }
        if (mTaskStartDate.getTag() == null) {
            showError("startDate");
            return false;
        }
        if (mTaskDueDate.getTag() == null) {
            showError("dueDate");
            return false;
        }

        Date startDate = (Date) mTaskStartDate.getTag();
        Date dueDate = (Date) mTaskDueDate.getTag();

        if (!startDate.before(dueDate)) {
            showError("dateError");
            return false;
        }

        if (mHorizontalEmployeeAdapter.getItemCount() == 0) {
            showError("employees");
            return false;
        }

        return true;
    }

    private void showError(String error) {
        switch (error) {
            case "name":
                break;
            case "description":
                break;
            case "startDate":
                break;
            case "dueDate":
                break;
            case "dateError":
                break;
            case "employees":
                AlertDialog.Builder builder = new AlertDialog.Builder(AddTaskActivity.this);
                builder.setMessage("There must be at least one employee assigned to this task.");
                builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.show();
                return;
        }
    }


    public void pickDate(View view) {
        AppUtils.showDatePicker(this, view, false);
    }


    @Override
    public void onEmployeeClick(int employeeRowID, int employeePosition) {
        Intent intent = new Intent(this, AddEmployeeActivity.class);
        intent.putExtra(AddEmployeeActivity.EMPLOYEE_ID_KEY, employeeRowID);
        startActivity(intent);
    }
}