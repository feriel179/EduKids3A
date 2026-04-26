CREATE DATABASE IF NOT EXISTS edukids
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE edukids;

CREATE TABLE IF NOT EXISTS quiz (
    id INT PRIMARY KEY AUTO_INCREMENT,
    titre VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    image_url TEXT,
    niveau VARCHAR(100) NOT NULL,
    categorie_age VARCHAR(100) NOT NULL DEFAULT '10 ans et plus',
    nombre_questions INT NOT NULL,
    duree_minutes INT NOT NULL,
    score_minimum INT NOT NULL,
    statut VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS question (
    id INT PRIMARY KEY AUTO_INCREMENT,
    quiz_id INT NOT NULL,
    intitule TEXT NOT NULL,
    type VARCHAR(100) NOT NULL,
    points INT NOT NULL,
    CONSTRAINT fk_question_quiz
        FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reponse (
    id INT PRIMARY KEY AUTO_INCREMENT,
    question_id INT NOT NULL,
    texte TEXT NOT NULL,
    correcte TINYINT(1) NOT NULL,
    CONSTRAINT fk_reponse_question
        FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quiz_result (
    id INT PRIMARY KEY AUTO_INCREMENT,
    quiz_id INT NOT NULL,
    final_score INT NOT NULL,
    earned_points INT NOT NULL,
    total_points INT NOT NULL,
    completed_at VARCHAR(100) NOT NULL,
    CONSTRAINT fk_quiz_result_quiz
        FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_question_quiz_id ON question(quiz_id);
CREATE INDEX idx_reponse_question_id ON reponse(question_id);
CREATE INDEX idx_quiz_result_quiz_id ON quiz_result(quiz_id);
