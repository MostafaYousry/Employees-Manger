package com.example.android.employeesmanagementapp.data.daos;

import com.example.android.employeesmanagementapp.data.entries.DepartmentEntry;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


/**
 * Departments entry Data Access Object
 * <p>
 * defines how to read/write operations are done to Departments Entry (database table)
 **/
@Dao
public interface DepartmentsDao {

    /**
     * load all departments
     *
     * @return list of DepartmentEntry objects wrapped with LiveData
     */
    @Query("Select * From departments")
    LiveData<List<DepartmentEntry>> loadDepartments();

    /**
     * load a department record by it's id
     *
     * @param departmentId : the department record's id
     * @return DepartmentEntry object wrapped with LiveData
     */
    @Query("SELECT * FROM departments WHERE department_id = :departmentId")
    LiveData<DepartmentEntry> loadDepartmentById(int departmentId);

    /**
     * insert a new department record
     *
     * @param departmentEntry
     */
    @Insert
    void addDepartment(DepartmentEntry departmentEntry);

    /**
     * delete an existing department record
     *
     * @param departmentEntry
     */
    @Delete
    void deleteDepartment(DepartmentEntry departmentEntry);

    /**
     * update columns of an existing department record
     *
     * @param departmentEntry
     */
    @Update
    void updateDepartment(DepartmentEntry departmentEntry);


}
