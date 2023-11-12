package com.example.app8;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class StudentListActivity extends AppCompatActivity {
    private Context context;
    private ListView listView;
    private CustomStudentListAdapter adapter; // Sử dụng CustomStudentListAdapter
    private ArrayList<String> studentList;
    private ArrayList<byte[]> imageDataList; // Danh sách dữ liệu hình ảnh
    private Button addButton;
    private Button deleteButton;
    private Button Export;
    private Button excel;
    private Button importjson;
    private Button exportjson;
    private TextView exportnotify;
    private Connection connection;
    private File fileexcel = new File("/storage/self/primary/Download"+"/Demo.xls");

    private boolean isMultipleChoiceMode = false;

    private final int ADD_STUDENT_REQUEST_CODE = 1;
    //
    private LinearLayout UIexport;
    //

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.student_list_activity);
        context = this;
        // Kết nối đến cơ sở dữ liệu bằng SQLConnection
        connection = SQLConnection.getConnection();
        TextView titleTextView = findViewById(R.id.titleTextView);
        listView = findViewById(R.id.listView);
        addButton = findViewById(R.id.addButton);
        deleteButton = findViewById(R.id.deleteButton);
        Export = findViewById(R.id.Export);
        UIexport = findViewById(R.id.UIExport);
        excel = findViewById(R.id.Excel);
        importjson = findViewById(R.id.imp_json);
        exportjson = findViewById(R.id.ex_json);
        Handler handler = new Handler();
        ImageView icon1 = findViewById(R.id.icon1);
        ImageView icon2 = findViewById(R.id.icon2);
        studentList = new ArrayList<>();
        imageDataList = new ArrayList<>(); // Khởi tạo danh sách dữ liệu hình ảnh
        adapter = new CustomStudentListAdapter(this, studentList, imageDataList); // Sử dụng CustomStudentListAdapter
        listView.setAdapter(adapter);

        // Nhận tên môn học từ Intent
        String subjectName = getIntent().getStringExtra("SubjectName");
        titleTextView.setText(subjectName);

        int backgroundValue = getBackgroundValueFromDatabase(subjectName);
        // Chuyển giá trị background thành tên tài nguyên drawable
        String drawableName = "background_" + backgroundValue; // Ví dụ: background_1
        // Lấy ID tài nguyên drawable từ tên
        int backgroundResId = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
        titleTextView.setBackgroundResource(backgroundResId);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Kiểm tra xem có phải là chế độ CHOICE_MODE_MULTIPLE không
                if (!isMultipleChoiceMode) {
                    // Lấy tên sinh viên khi bạn bấm vào item trong ListView
                    String studentName = studentList.get(position);

                    // Truy vấn cơ sở dữ liệu để lấy thông tin sinh viên dựa vào tên
                    StudentInfo studentInfo = getStudentInfoFromDatabase(studentName);

                    // Kiểm tra nếu có dữ liệu sinh viên
                    if (studentInfo != null) {
                        // Tạo một AlertDialog.Builder
                        AlertDialog.Builder builder = new AlertDialog.Builder(StudentListActivity.this);
                        builder.setTitle("Thông tin sinh viên");

                        // Tạo một View để hiển thị thông tin sinh viên
                        View dialogView = getLayoutInflater().inflate(R.layout.student_info_dialog, null);

                        // Lấy các thành phần View trong dialogView
                        TextView nameTextView = dialogView.findViewById(R.id.nameTextView);
                        TextView dobTextView = dialogView.findViewById(R.id.dobTextView);
                        TextView codeTextView = dialogView.findViewById(R.id.codeTextView);
                        ImageView imageView = dialogView.findViewById(R.id.imageView);

                        // Đặt thông tin sinh viên vào các View
                        nameTextView.setText(studentInfo.getName());
                        dobTextView.setText(studentInfo.getDateOfBirth());
                        codeTextView.setText(studentInfo.getCode());
                        byte[] imageData = studentInfo.getImageData();
                        if (imageData != null) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                            imageView.setImageBitmap(bitmap);
                        }

                        // Đặt View vào AlertDialog
                        builder.setView(dialogView);

                        // Tạo và hiển thị AlertDialog
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                }
            }
        });
        //Chuyển đến phần Export
        Export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(UIexport.getVisibility()==View.INVISIBLE){
                    UIexport.setVisibility(View.VISIBLE);
                }else {
                        UIexport.setVisibility(View.INVISIBLE);
                }
            }
        });
        excel.setOnClickListener(new View.OnClickListener(){
            List<StudentInfo> Stlist;
            @Override
            public void onClick(View view) {
                if (connection != null) {
                    try {
                        Statement statement = connection.createStatement();
                        ResultSet resultSet = statement.executeQuery("SELECT name_student, code_student, date_of_birth, ImageData FROM STUDENT_LIST");

                        // Tạo workbook và sheet
                        HSSFWorkbook workbook = new HSSFWorkbook();
                        HSSFSheet sheet = workbook.createSheet("Data");

                        // Lấy thông tin metadata của kết quả truy vấn
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        // Tạo row cho tiêu đề cột
                        HSSFRow headerRow = sheet.createRow(0);
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            headerRow.createCell(i - 1).setCellValue(columnName);
                        }

                        // Tạo row cho từng bản ghi trong kết quả truy vấn
                        int rowNum = 1;
                        while (resultSet.next()) {
                            HSSFRow row = sheet.createRow(rowNum++);
                            for (int i = 1; i <= columnCount; i++) {
                                Object value = resultSet.getObject(i);
                                row.createCell(i - 1).setCellValue(value != null ? value.toString() : "");
                            }
                        }

                        // Lưu workbook vào tệp tin
                        try (FileOutputStream fileOut = new FileOutputStream(fileexcel)) {
                            workbook.write(fileOut);
                            Toast.makeText(StudentListActivity.this,"Export Excel Success",Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(StudentListActivity.this,"Error Exporting to Excel",Toast.LENGTH_SHORT).show();
                        }

                        // Đóng workbook, ResultSet, statement và connection
                        try {
                            workbook.close();
                            resultSet.close();
                            statement.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        importjson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(connection!=null){
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/json");
                    startActivity(intent);
                }
            }
        });
        exportjson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(connection!=null){
                    try{
                        Statement statement = connection.createStatement();
                        ResultSet resultSet = statement.executeQuery("SELECT name_student, code_student, date_of_birth, ImageData FROM STUDENT_LIST");
                        //create list
                        List<StudentInfo> st = new ArrayList<>();
                        while(resultSet.next()){
                            String name = resultSet.getString("name_student");
                            String code = resultSet.getString("code_student");
                            String dateOfBirth = resultSet.getString("date_of_birth");
                            byte[] imageData = resultSet.getBytes("ImageData");
                            StudentInfo st1 = new StudentInfo(name,code,dateOfBirth,imageData);
                            st.add(st1);
                        }
                        resultSet.close();
                        // Export the data to JSON
                        Gson gson = new Gson();
                        String jsonData = gson.toJson(st);
                        try {
                            FileWriter writer = new FileWriter("/storage/self/primary/Download/Demo.json");
                            writer.write(jsonData);
                            writer.close();
                            statement.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(StudentListActivity.this,"Exported Success",Toast.LENGTH_SHORT).show();
                    }catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(StudentListActivity.this,"Error exporting to JSON",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Trước khi hiển thị AddStudentDialogFragment, đính kèm subjectName vào Bundle
                FragmentManager fragmentManager = getSupportFragmentManager();
                StudentAddDialogFragment dialogFragment = new StudentAddDialogFragment();
                Bundle args = new Bundle();
                args.putString("SubjectName", subjectName); // Đính kèm subjectName vào Bundle
                dialogFragment.setArguments(args);
                dialogFragment.setActivityReference(StudentListActivity.this);
                dialogFragment.show(fragmentManager, "AddStudentDialogFragment");
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Kiểm tra trạng thái chọn của ListView
                boolean isMultipleChoice = (listView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE);
                isMultipleChoiceMode = !isMultipleChoice;
                // Hiển thị hoặc ẩn biểu tượng 1 và biểu tượng 2
                int visibility = isMultipleChoice ? View.GONE : View.VISIBLE;
                findViewById(R.id.icon1).setVisibility(visibility);
                findViewById(R.id.icon2).setVisibility(visibility);

                // Chuyển đổi giữa SingleChoice và MultipleChoice
                if (isMultipleChoice) {
                    listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                } else {
                    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                }

                // Tạo một adapter mới với danh sách hiện tại và gán lại vào ListView
                ArrayAdapter<String> newAdapter = new ArrayAdapter<>(StudentListActivity.this,
                        android.R.layout.simple_list_item_multiple_choice, studentList);
                if (isMultipleChoice) {
                    listView.setAdapter(adapter);
                } else {
                    listView.setAdapter(newAdapter);
                }
            }
        });
        icon1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ẩn biểu tượng 1 và biểu tượng 2
                findViewById(R.id.icon1).setVisibility(View.GONE);
                findViewById(R.id.icon2).setVisibility(View.GONE);
                // Cập nhật lại adapter
                listView.setAdapter(adapter);
            }
        });
        icon2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Kiểm tra trạng thái chọn của ListView
                boolean isMultipleChoice = (listView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE);

                if (isMultipleChoice) {
                    // Lấy danh sách các sinh viên đã chọn
                    SparseBooleanArray checkedPositions = listView.getCheckedItemPositions();
                    ArrayList<Integer> positionsToRemove = new ArrayList<>();

                    // Tìm các vị trí cần xóa và thêm vào danh sách positionsToRemove
                    for (int i = 0; i < checkedPositions.size(); i++) {
                        int position = checkedPositions.keyAt(i);
                        if (checkedPositions.get(position)) {
                            positionsToRemove.add(position);
                        }
                    }

                    // Xóa các sinh viên đã chọn từ cơ sở dữ liệu
                    for (int i = positionsToRemove.size() - 1; i >= 0; i--) {
                        int position = positionsToRemove.get(i);
                        String studentName = studentList.get(position);
                        boolean isDeleted = deleteStudent(studentName);
                        if (isDeleted) {
                            // Xóa thành công, cập nhật lại danh sách hiển thị
                            studentList.remove(position);
                            imageDataList.remove(position); // Xóa dữ liệu hình ảnh tương ứng
                        }
                    }

                    // Kết thúc chế độ xóa (chuyển về CustomMode)
                    findViewById(R.id.icon1).setVisibility(View.GONE);
                    findViewById(R.id.icon2).setVisibility(View.GONE);
                    listView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }
            }
        });

        // Tải danh sách sinh viên từ cơ sở dữ liệu dựa trên tên môn học
        loadStudentList(subjectName);
    }

    private boolean deleteStudent(String studentName) {
        if (connection != null) {
            try {
                // Thực hiện truy vấn SQL để xóa sinh viên dựa trên tên
                String query = "DELETE FROM STUDENT_LIST WHERE name_student = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, studentName);

                int rowsDeleted = preparedStatement.executeUpdate();
                preparedStatement.close();

                return rowsDeleted > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // Hàm này thực hiện tải danh sách sinh viên từ cơ sở dữ liệu và cập nhật ListView
    private void loadStudentList(String subjectName) {
        if (connection != null) {
            try {
                studentList.clear();
                imageDataList.clear(); // Xóa danh sách dữ liệu hình ảnh
                String query = "SELECT name_student, ImageData FROM STUDENT_LIST WHERE class_id IN (SELECT id FROM CLASS WHERE name_subject = ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, subjectName);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    String studentName = resultSet.getString("name_student");
                    studentList.add(studentName);

                    byte[] imageData = resultSet.getBytes("ImageData");
                    imageDataList.add(imageData); // Thêm dữ liệu hình ảnh vào danh sách
                }
                resultSet.close();
                preparedStatement.close();
                adapter.notifyDataSetChanged();
            } catch (SQLException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi lấy dữ liệu từ cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Không thể kết nối đến cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
        }
    }

    private int getBackgroundValueFromDatabase(String className) {
        int backgroundValue = -1; // Giá trị mặc định

        if (connection != null) {
            try {
                // Truy vấn SQL để lấy background từ bảng CLASS dựa trên tên lớp học
                String getBackgroundQuery = "SELECT background FROM CLASS WHERE name_subject = ?";
                PreparedStatement getBackgroundStatement = connection.prepareStatement(getBackgroundQuery);
                getBackgroundStatement.setString(1, className);
                ResultSet backgroundResult = getBackgroundStatement.executeQuery();

                // Kiểm tra xem có dữ liệu trả về không
                if (backgroundResult.next()) {
                    backgroundValue = backgroundResult.getInt("background");
                }

                getBackgroundStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return backgroundValue;
    }
    private StudentInfo getStudentInfoFromDatabase(String studentName) {
        StudentInfo studentInfo = null;

        if (connection != null) {
            try {
                ResultSet resultSet;
                PreparedStatement preparedStatement;
                // Truy vấn SQL để lấy thông tin sinh viên từ bảng STUDENT_LIST dựa trên tên sinh viên
                    String query = "SELECT name_student, code_student, date_of_birth, ImageData FROM STUDENT_LIST WHERE name_student = ?";
                    preparedStatement = connection.prepareStatement(query);
                    resultSet = preparedStatement.executeQuery();
                    preparedStatement.setString(1, studentName);

                // Kiểm tra xem có dữ liệu trả về không
                if (resultSet.next()) {
                    String name = resultSet.getString("name_student");
                    String code = resultSet.getString("code_student");
                    String dateOfBirth = resultSet.getString("date_of_birth");
                    byte[] imageData = {0,0,0};

                    // Tạo đối tượng StudentInfo từ dữ liệu lấy được
                    studentInfo = new StudentInfo(name, code, dateOfBirth, imageData);
                }

                resultSet.close();
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return studentInfo;
    }


    void loadAndUpdateStudentList(String subjectName) {
        loadStudentList(subjectName);
    }
}
