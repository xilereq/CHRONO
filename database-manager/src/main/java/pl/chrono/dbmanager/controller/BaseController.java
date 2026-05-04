package pl.chrono.dbmanager.controller;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public abstract class BaseController<T, K> {

  protected final JpaRepository<T, K> repository;

  public BaseController(JpaRepository<T, K> repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<T> getAll() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public ResponseEntity<T> getById(@PathVariable K id) {
    return repository.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public T create(@RequestBody T object) {
    return repository.save(object);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable K id) {
    if (repository.existsById(id)) {
      repository.deleteById(id);
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
  }
}
