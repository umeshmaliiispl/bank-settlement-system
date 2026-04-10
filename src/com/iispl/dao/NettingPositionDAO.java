package com.iispl.dao;

import java.util.List;
import com.iispl.entity.NettingPosition;

public interface NettingPositionDAO {

    void saveAll(List<NettingPosition> positions);

}