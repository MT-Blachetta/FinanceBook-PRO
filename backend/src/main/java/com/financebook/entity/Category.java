package com.financebook.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.financebook.util.Constants;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A single tag within a 'CategoryType' taxonomy tree.
 * 
 * The self-referencing 'parent' allows arbitrary depth without cycles.
 * Categories can be nested hierarchically with unlimited depth.
 */
@Entity
@Table(name = "category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = Constants.MAX_CATEGORY_NAME_LENGTH)
    @Column(nullable = false, length = Constants.MAX_CATEGORY_NAME_LENGTH)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private CategoryType type;
    
    // Self-referencing parent pointer (null for root nodes)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;
    
    // No cascade / orphanRemoval: deleting or detaching a parent must never
    // implicitly wipe out an entire subtree. CategoryService re-parents
    // children explicitly before a category is removed.
    @OneToMany(mappedBy = "parent")
    @JsonIgnore
    @Builder.Default
    private List<Category> children = new ArrayList<>();
    
    @Column(name = "icon_file", length = 255)
    private String iconFile;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
