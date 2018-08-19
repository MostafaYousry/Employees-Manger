package com.example.android.employeesmanagementapp.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.android.employeesmanagementapp.NotificationService;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AddTaskActivity extends BaseAddActivity implements EmployeesAdapter.EmployeeItemClickListener {

    public static final String TASK_ID_KEY = "task_id";
    public static final String TASK_IS_COMPLETED_KEY = "task_is_completed";
    private static final boolean DEFAULT_TASK_IS_COMPLETED = false;
    private boolean mTaskIsCompleted;
    private static final int DEFAULT_TASK_ID = -1;
    boolean departmentsLoaded = false;
    private EditText mTaskTitle;
    private EditText mTaskDescription;
    private TextView mTaskStartDate;
    private TextView mTaskDueDate;
    private TextView mTaskStartTime;
    private TextView mTaskDueTime;
    private Spinner mTaskDepartment;
    private RecyclerView mTaskEmployeesRV;
    private int mTaskId;
    private TaskEntry mOldTaskEntry;
    private HorizontalEmployeeAdapter mHorizontalEmployeeAdapter;
    private DepartmentsArrayAdapter mDepartmentsArrayAdapter;
    private AppDatabase mDb;
    private AddNewTaskViewModel mViewModel;
    private int clickedTaskDepId = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        //bind views
        mTaskTitle = findViewById(R.id.task_title);
        mTaskDescription = findViewById(R.id.task_description);
        mTaskStartDate = findViewById(R.id.task_start_date);
        mTaskDueDate = findViewById(R.id.task_due_date);
        mTaskStartTime = findViewById(R.id.task_start_date_time);
        mTaskDueTime = findViewById(R.id.task_due_date_time);
        mTaskDepartment = findViewById(R.id.task_department);
        Toolbar mToolbar = findViewById(R.id.toolbar);
        mTaskEmployeesRV = findViewById(R.id.task_employees_rv);

        mDb = AppDatabase.getInstance(this);

        //check if activity was opened from a click on rv item or from the fab
        Intent intent = getIntent();
        if (intent != null) {
            mTaskId = intent.getIntExtra(TASK_ID_KEY, DEFAULT_TASK_ID);
            mTaskIsCompleted = intent.getBooleanExtra(TASK_IS_COMPLETED_KEY, DEFAULT_TASK_IS_COMPLETED);
        }

        mViewModel = ViewModelProviders.of(this, new TaskIdFact(mDb, mTaskId)).get(AddNewTaskViewModel.class);


        //set toolbar as actionbar
        setSupportActionBar(mToolbar);

        //set toolbar home icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);


        mDepartmentsArrayAdapter = new DepartmentsArrayAdapter(this, AppUtils.dpToPx(this, 12), AppUtils.dpToPx(this, 8), AppUtils.dpToPx(this, 0), AppUtils.dpToPx(this, 8), R.style.detailActivitiesTextStyle);


        if (!mTaskIsCompleted) {
            LiveData<List<DepartmentEntry>> departments = mViewModel.allDepartments;
            departments.observe(this, departmentEntries -> {
                mDepartmentsArrayAdapter.setData(departmentEntries);
                departmentsLoaded = true;
                if (clickedTaskDepId != -1) {
                    mTaskDepartment.setSelection(mDepartmentsArrayAdapter.getPositionForItemId(clickedTaskDepId));
                }
            });
        } else {
            Snackbar.make(findViewById(R.id.coordinator), "Completed tasks are view only", Snackbar.LENGTH_LONG).show();
        }


        mTaskDepartment.setAdapter(mDepartmentsArrayAdapter);


        setUpToolBar();

        if (mTaskId == DEFAULT_TASK_ID) {
            clearViews();
        } else {
            final LiveData<TaskEntry> task = mViewModel.taskEntry;
            task.observe(this, new Observer<TaskEntry>() {
                @Override
                public void onChanged(@Nullable TaskEntry taskEntry) {
                    task.removeObserver(this);
                    populateUI(taskEntry);
                }
            });
        }


        setUpTaskEmployeesRV();

        mTaskDueDate.setOnClickListener(this::showDatePicker);

        mTaskStartDate.setOnClickListener(this::showDatePicker);

        mTaskStartTime.setOnClickListener(this::showTimePicker);

        mTaskDueTime.setOnClickListener(this::showTimePicker);

        ViewCompat.setNestedScrollingEnabled(mTaskEmployeesRV, false);

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
            depId = mViewModel.taskEntry.getValue().getDepartmentID();
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
            arr[i] = AppUtils.getFullEmployeeName(employeeEntries.get(i));
        }

        return arr;
    }


    private void setUpTaskEmployeesRV() {

        mTaskEmployeesRV.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        mTaskEmployeesRV.setLayoutManager(layoutManager);

        mHorizontalEmployeeAdapter = new HorizontalEmployeeAdapter(this, this, true, mTaskId);
        mTaskEmployeesRV.setAdapter(mHorizontalEmployeeAdapter);

        if (mTaskId != DEFAULT_TASK_ID) {
            mViewModel.taskEmployees.observe(this, mHorizontalEmployeeAdapter::submitList);
        }
    }

    private void clearViews() {
        mTaskTitle.setText("");
        mTaskDescription.setText("");
        mTaskStartDate.setText("");
        mTaskDueDate.setText("");
        mTaskStartTime.setText("");
        mTaskDueTime.setText("");
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

        mOldTaskEntry = taskEntry;

        clickedTaskDepId = taskEntry.getDepartmentID();
        if (mTaskIsCompleted) {
            mDb.departmentsDao().loadDepartmentById(clickedTaskDepId).observe(this, new Observer<DepartmentEntry>() {
                @Override
                public void onChanged(DepartmentEntry departmentEntry) {
                    if (departmentEntry != null) {
                        List<DepartmentEntry> list = new ArrayList<>(1);
                        list.add(departmentEntry);
                        mDepartmentsArrayAdapter.setData(list);
                        mTaskDepartment.setSelection(0);
                    }

                }
            });


            disableViewClicks();

        } else {
            if (departmentsLoaded)
                mTaskDepartment.setSelection(mDepartmentsArrayAdapter.getPositionForItemId(taskEntry.getDepartmentID()));

            mTaskDepartment.setEnabled(false);

            // TODO: 8/15/18
//        mTaskDepartment.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(AddTaskActivity.this);
//                builder.setMessage("Can't change department of an already running taskEntry.If you want you can delete this taskEntry and create a new one.");
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

        mTaskTitle.setText(taskEntry.getTaskTitle());
        mTaskDescription.setText(taskEntry.getTaskDescription());

        mTaskStartDate.setText(AppUtils.getFriendlyDate(taskEntry.getTaskStartDate()));
        mTaskStartDate.setTag(taskEntry.getTaskStartDate());

        mTaskDueDate.setText(AppUtils.getFriendlyDate(taskEntry.getTaskDueDate()));
        mTaskDueDate.setTag(taskEntry.getTaskDueDate());

        mTaskStartTime.setText(AppUtils.getFriendlyTime(taskEntry.getTaskStartDate()));
        mTaskStartTime.setTag(taskEntry.getTaskStartDate());

        mTaskDueTime.setText(AppUtils.getFriendlyTime(taskEntry.getTaskDueDate()));
        mTaskDueTime.setTag(taskEntry.getTaskDueDate());


    }

    private void disableViewClicks() {


        mTaskTitle.setEnabled(false);
        mTaskTitle.setFocusable(false);

        mTaskDescription.setEnabled(false);
        mTaskDescription.setFocusable(false);

        mTaskStartDate.setEnabled(false);
        mTaskStartDate.setFocusable(false);

        mTaskDueDate.setEnabled(false);
        mTaskDueDate.setFocusable(false);

        mTaskStartTime.setEnabled(false);
        mTaskStartTime.setFocusable(false);

        mTaskDueTime.setEnabled(false);
        mTaskDueTime.setFocusable(false);

        mTaskDepartment.setEnabled(false);
        mTaskDepartment.setFocusable(false);

        findViewById(R.id.add_employees_to_task_button).setClickable(false);

    }

    private void setUpToolBar() {
        if (!mTaskIsCompleted)
            if (mTaskId == DEFAULT_TASK_ID) {
                getSupportActionBar().setTitle(getString(R.string.add_task));
            } else {
                getSupportActionBar().setTitle(getString(R.string.edit_task));
            }
        else
            getSupportActionBar().setTitle(getString(R.string.view_task));
    }


    @Override
    protected void onStop() {
        super.onStop();
        Intent intent = new Intent(this, NotificationService.class);
        intent.putExtra("task id", mTaskId);
        intent.putExtra("task due date", AppUtils.getChosenDateAndTime(((Date) mTaskDueDate.getTag()), ((Date) mTaskDueTime.getTag())).getTime() - new Date().getTime());
        intent.putExtra("app is destroyed", false);
        startService(intent);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        if (mTaskIsCompleted)
            menu.removeItem(R.id.action_save);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                save();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean isDataValid() {

        boolean valid = true;

        if (TextUtils.isEmpty(mTaskTitle.getText())) {
            updateErrorVisibility("title", true);
            valid = false;
        } else {
            updateErrorVisibility("title", false);
        }

        if (mTaskTitle.getText().length() > 40) {
            valid = false;
        }
        Date startDate = (Date) mTaskStartDate.getTag();
        Date dueDate = (Date) mTaskDueDate.getTag();


        if (startDate == null) {
            updateErrorVisibility("startDate", true);
            valid = false;
        } else {
            updateErrorVisibility("startDate", false);
        }

        if (dueDate == null) {
            updateErrorVisibility("dueDate", true);
            valid = false;
        } else {
            updateErrorVisibility("dueDate", false);
        }

        if (mHorizontalEmployeeAdapter.getItemCount() == 0) {
            updateErrorVisibility("employees", true);
            valid = false;
        }

        return valid;
    }

    @Override
    protected void updateErrorVisibility(String key, boolean show) {
        if (show)
            switch (key) {
                case "title":
                    ((TextInputLayout) findViewById(R.id.task_title_TIL)).setError(getString(R.string.required_field));
                    break;
                case "startDate":
                    ((TextInputLayout) findViewById(R.id.task_start_date_TIL)).setError(getString(R.string.required_field));
                    break;
                case "dueDate":
                    ((TextInputLayout) findViewById(R.id.task_due_date_TIL)).setError(getString(R.string.required_field));
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
            }
        else
            switch (key) {
                case "title":
                    ((TextInputLayout) findViewById(R.id.task_title_TIL)).setHelperText(getString(R.string.required_field));

                    break;
                case "startDate":
                    ((TextInputLayout) findViewById(R.id.task_start_date_TIL)).setHelperText(getString(R.string.required_field));
                    break;
                case "dueDate":
                    ((TextInputLayout) findViewById(R.id.task_due_date_TIL)).setHelperText(getString(R.string.required_field));
                    break;
            }
    }

    @Override
    protected void save() {
        if (isDataValid()) {
            final TaskEntry newTask = getTaskEntry();

            AppExecutor.getInstance().diskIO().execute(() -> {
                if (mTaskId == DEFAULT_TASK_ID) {
                    mTaskId = (int) mDb.tasksDao().addTask(newTask);
                } else {
                    mDb.tasksDao().updateTask(newTask);
                }
            });


            final List<EmployeeEntry> addedEmployees = mHorizontalEmployeeAdapter.getAddedEmployees();
            if (addedEmployees != null && !addedEmployees.isEmpty())
                AppExecutor.getInstance().diskIO().execute(() -> {
                    for (int i = 0; i < addedEmployees.size(); i++) {
                        EmployeesTasksEntry newEmployeeTask = new EmployeesTasksEntry(addedEmployees.get(i).getEmployeeID(), mTaskId);
                        mDb.employeesTasksDao().addEmployeeTask(newEmployeeTask);
                    }
                });
            finish();
        }
    }


    @Override
    public void onEmployeeClick(int employeeRowID, int employeePosition) {
        Intent intent = new Intent(this, AddEmployeeActivity.class);
        intent.putExtra(AddEmployeeActivity.EMPLOYEE_ID_KEY, employeeRowID);
        startActivity(intent);
    }

    private TaskEntry getTaskEntry() {
        int departmentId = (int) mTaskDepartment.getSelectedView().getTag();
        String taskTitle = mTaskTitle.getText().toString();
        String taskDescription = mTaskDescription.getText().toString();
        Date taskDueDate = AppUtils.getChosenDateAndTime(((Date) mTaskDueDate.getTag()), ((Date) mTaskDueTime.getTag()));
        Date taskStartDate = AppUtils.getChosenDateAndTime(((Date) mTaskStartDate.getTag()), ((Date) mTaskStartTime.getTag()));

        if (mTaskId == DEFAULT_TASK_ID)
            return new TaskEntry(departmentId, taskTitle, taskDescription, taskStartDate, taskDueDate);
        else
            return new TaskEntry(mTaskId, departmentId, taskTitle, taskDescription, taskStartDate, taskDueDate, mOldTaskEntry.getTaskRating(), mOldTaskEntry.isTaskIsCompleted(), mOldTaskEntry.getTaskColorResource());
    }

    @Override
    protected boolean fieldsChanged() {
        if (mTaskId == DEFAULT_TASK_ID) {
            if (!TextUtils.isEmpty(mTaskTitle.getText()))
                return true;

            if (!TextUtils.isEmpty(mTaskDescription.getText()))
                return true;

            if (mTaskStartDate.getTag() != null)
                return true;

            if (mTaskDueDate.getTag() != null)
                return true;

            if (mTaskStartTime.getTag() != null)
                return true;

            if (mTaskDueTime.getTag() != null)
                return true;

            if (mTaskDepartment.getSelectedItemPosition() != 0)
                return true;
        } else
            return !mOldTaskEntry.equals(getTaskEntry()) || mHorizontalEmployeeAdapter.didChangeOccur();


        return false;
    }

    @Override
    protected void showDiscardChangesDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Discard changes");
        builder.setMessage("All changes will be discarded.");
        builder.setNegativeButton("DISCARD", (dialogInterface, i) -> {
            dialogInterface.dismiss();
            finish();
        });

        builder.setPositiveButton("SAVE", (dialogInterface, i) -> {
            save();
            dialogInterface.dismiss();
        });
        builder.show();
    }

}