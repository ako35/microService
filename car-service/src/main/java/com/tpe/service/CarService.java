package com.tpe.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.tpe.domain.Car;
import com.tpe.dto.AppLogDTO;
import com.tpe.dto.CarDTO;
import com.tpe.dto.CarRequest;
import com.tpe.enums.AppLogLevel;
import com.tpe.exception.ResourceNotFoundException;
import com.tpe.repository.CarRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarService {

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RestTemplate restTemplate;

    // register olan microservice leri bulmamizi saglayacak yapiyi enjekte ettik
    @Autowired
    private EurekaClient eurekaClient;

    public void saveCar(CarRequest carRequest) {
        Car car= modelMapper.map(carRequest, Car.class);
        carRepository.save(car);

        // log service ile irtibata geciliyor.
        InstanceInfo instanceInfo=eurekaClient.getApplication("log-service").getInstances().get(0);
        String baseUrl=instanceInfo.getHomePageUrl();
        String path="/log";
        String servicePath=baseUrl+path;

        AppLogDTO appLogDTO=new AppLogDTO();
        appLogDTO.setLevel(AppLogLevel.INFO.getName());
        appLogDTO.setDescription("Save a Car: " + car.getId());
        appLogDTO.setTime(LocalDateTime.now());

        ResponseEntity<String> logResponse=restTemplate.postForEntity(servicePath,appLogDTO,String.class);
        if (!(logResponse.getStatusCode()== HttpStatus.CREATED)){
            throw new ResourceNotFoundException("Log not created");
        }
    }

    public List<CarDTO> getAllCars() {
        List<Car> carList=carRepository.findAll();
        List<CarDTO> carDTOList=carList.stream().map(this::mapCarToCarDTO).collect(Collectors.toList());
        return carDTOList;
    }

    private CarDTO mapCarToCarDTO(Car car){
        CarDTO carDTO=modelMapper.map(car, CarDTO.class);
        return carDTO;
    }

    public CarDTO getById(Long id) {
        Car car=carRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Car not found with id: " +id));
        CarDTO carDTO=mapCarToCarDTO(car);
        return carDTO;
    }
}
