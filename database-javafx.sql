CREATE DATABASE IF NOT EXISTS javafx;
USE javafx;

CREATE TABLE IF NOT EXISTS category (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS produit (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    prix DOUBLE NOT NULL,
    stock INT NOT NULL,
    category_id INT NOT NULL,
    CONSTRAINT fk_produit_category
        FOREIGN KEY (category_id) REFERENCES category(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS `user` (
    id INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(180) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO category (nom, description)
SELECT 'Informatique', 'Ordinateurs, accessoires et composants'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE nom = 'Informatique');

INSERT INTO category (nom, description)
SELECT 'Maison', 'Decoration et equipements'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE nom = 'Maison');

INSERT INTO category (nom, description)
SELECT 'Sport', 'Produits pour l''activite physique'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE nom = 'Sport');

INSERT INTO produit (nom, description, image_url, prix, stock, category_id)
SELECT 'Laptop Pro', 'PC portable haute performance', 'https://example.com/laptop-pro.jpg', 3899, 12, c.id
FROM category c
WHERE c.nom = 'Informatique'
  AND NOT EXISTS (SELECT 1 FROM produit WHERE nom = 'Laptop Pro');

INSERT INTO produit (nom, description, image_url, prix, stock, category_id)
SELECT 'Chaise Gamer', 'Chaise ergonomique premium', 'https://example.com/chaise-gamer.jpg', 799, 8, c.id
FROM category c
WHERE c.nom = 'Maison'
  AND NOT EXISTS (SELECT 1 FROM produit WHERE nom = 'Chaise Gamer');

INSERT INTO `user` (email, role, password_hash, first_name, last_name, is_active)
SELECT 'admin@edukids.local', 'ROLE_ADMIN', '246945a4b9c9af5d151d4a7c287a996fa2b578902f5683f4b9878aef94347ba8', 'Admin', 'EduKids', TRUE
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE email = 'admin@edukids.local');

INSERT INTO `user` (email, role, password_hash, first_name, last_name, is_active)
SELECT 'parent@edukids.local', 'ROLE_PARENT', '246945a4b9c9af5d151d4a7c287a996fa2b578902f5683f4b9878aef94347ba8', 'Marie', 'Martin', TRUE
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE email = 'parent@edukids.local');
