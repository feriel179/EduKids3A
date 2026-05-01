package tn.esprit.controllers.admin;

import com.edukids.controllers.DashboardController;
import tn.esprit.models.Course;
import tn.esprit.models.Lesson;

public final class AdminModuleNavigator {
    private AdminModuleNavigator() {
    }

    public static void showCourses() {
        DashboardController dashboard = activeDashboard();
        if (dashboard != null) {
            dashboard.showCourses();
            return;
        }
        AdminShellController shell = AdminShellController.getInstance();
        if (shell != null) {
            shell.showCourses();
        }
    }

    public static void showCreateCourse() {
        DashboardController dashboard = activeDashboard();
        if (dashboard != null) {
            dashboard.showCreateCourse();
            return;
        }
        AdminShellController shell = AdminShellController.getInstance();
        if (shell != null) {
            shell.showCreateCourse();
        }
    }

    public static void showEditCourse(Course course) {
        DashboardController dashboard = activeDashboard();
        if (dashboard != null) {
            dashboard.showEditCourse(course);
            return;
        }
        AdminShellController shell = AdminShellController.getInstance();
        if (shell != null) {
            shell.showEditCourse(course);
        }
    }

    public static void showCourseSuccess(Course course, boolean updated) {
        DashboardController dashboard = activeDashboard();
        if (dashboard != null) {
            dashboard.showCourseSuccess(course, updated);
            return;
        }
        AdminShellController shell = AdminShellController.getInstance();
        if (shell != null) {
            shell.showCourseSuccess(course, updated);
        }
    }

    public static void showLessons() {
        DashboardController dashboard = activeDashboard();
        if (dashboard != null) {
            dashboard.showLessons();
            return;
        }
        AdminShellController shell = AdminShellController.getInstance();
        if (shell != null) {
            shell.showLessons();
        }
    }

    public static void showCreateLesson(Course preselectedCourse) {
        DashboardController dashboard = activeDashboard();
        if (dashboard != null) {
            dashboard.showCreateLesson(preselectedCourse);
            return;
        }
        AdminShellController shell = AdminShellController.getInstance();
        if (shell != null) {
            shell.showCreateLesson(preselectedCourse);
        }
    }

    public static void showEditLesson(Lesson lesson) {
        DashboardController dashboard = activeDashboard();
        if (dashboard != null) {
            dashboard.showEditLesson(lesson);
            return;
        }
        AdminShellController shell = AdminShellController.getInstance();
        if (shell != null) {
            shell.showEditLesson(lesson);
        }
    }

    public static void showLessonSuccess(Lesson lesson, boolean updated) {
        DashboardController dashboard = activeDashboard();
        if (dashboard != null) {
            dashboard.showLessonSuccess(lesson, updated);
            return;
        }
        AdminShellController shell = AdminShellController.getInstance();
        if (shell != null) {
            shell.showLessonSuccess(lesson, updated);
        }
    }

    private static DashboardController activeDashboard() {
        DashboardController dashboard = DashboardController.getInstance();
        return dashboard != null && dashboard.isAttachedToShowingWindow() ? dashboard : null;
    }
}
