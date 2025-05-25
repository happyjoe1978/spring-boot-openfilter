package com.orangstar.server.repository;

import com.orangstar.server.entity.Note;
import com.orangstar.server.openfilter.OFRepository;
import com.orangstar.server.openfilter.OpenFilterRepository;

@OFRepository
public interface NoteRepository extends OpenFilterRepository<Note, String> {

}
